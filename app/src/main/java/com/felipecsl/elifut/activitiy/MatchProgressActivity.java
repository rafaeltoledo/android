package com.felipecsl.elifut.activitiy;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.felipecsl.elifut.BuildConfig;
import com.felipecsl.elifut.R;
import com.felipecsl.elifut.ResponseObserver;
import com.felipecsl.elifut.match.MatchResultController;
import com.felipecsl.elifut.models.Club;
import com.felipecsl.elifut.models.Goal;
import com.felipecsl.elifut.models.LeagueRound;
import com.felipecsl.elifut.models.Match;
import com.felipecsl.elifut.models.MatchResult;
import com.felipecsl.elifut.preferences.LeagueDetails;
import com.felipecsl.elifut.preferences.UserPreferences;
import com.felipecsl.elifut.widget.FractionView;
import com.squareup.picasso.Picasso;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.BindString;
import butterknife.ButterKnife;
import butterknife.OnClick;
import icepick.State;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

public class MatchProgressActivity extends ElifutActivity {
  private static final String TAG = MatchProgressActivity.class.getSimpleName();
  private static final String EXTRA_ROUND = "EXTRA_ROUND";

  @Bind(R.id.toolbar) Toolbar toolbar;
  @Bind(R.id.img_team_home) ImageView imgTeamHome;
  @Bind(R.id.img_team_away) ImageView imgTeamAway;
  @Bind(R.id.txt_team_home) TextView txtTeamHome;
  @Bind(R.id.txt_team_away) TextView txtTeamAway;
  @Bind(R.id.txt_match_events) TextView txtMatchEvents;
  @Bind(R.id.txt_team_home_goals) TextView txtTeamHomeGoals;
  @Bind(R.id.txt_team_away_goals) TextView txtTeamAwayGoals;
  @Bind(R.id.fractionView) FractionView fractionView;
  @Bind(R.id.fab_play_pause) FloatingActionButton playPauseButton;
  @Bind(R.id.fab_done) FloatingActionButton doneButton;
  @BindString(R.string.end_first_half) String strEndOfFirstHalf;
  @BindString(R.string.end_match) String strEndOfMatch;

  @Inject UserPreferences userPreferences;
  @Inject LeagueDetails leagueDetails;

  @State LeagueRound round;
  @State boolean isRunning;
  @State int elapsedMinutes;

  private final CompositeSubscription subscriptions = new CompositeSubscription();
  private String finalScoreMessage;
  private Match match;
  private MatchResult matchResult;
  private final Observer<Club> observer = new ResponseObserver<Club>(this, TAG, "Failed to load club") {
    @Override public void onNext(Club response) {
      fillClubInfos(response);
    }

    @Override public void onCompleted() {
      if (!matchResult.isDraw()) {
        //noinspection ConstantConditions
        finalScoreMessage = matchResult.winner().abbrev_name() + " is the winner. Final score "
            + matchResult.finalScore() + ".";
      } else {
        finalScoreMessage = "Game draw. Final score " + matchResult.finalScore() + ".";
      }
      if (BuildConfig.DEBUG) {
        Log.d(TAG, finalScoreMessage);
      }

      startTimer();
    }
  };

  public static Intent newIntent(Context context, LeagueRound round) {
    return new Intent(context, MatchProgressActivity.class)
        .putExtra(EXTRA_ROUND, round);
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_match_progress);
    ButterKnife.bind(this);
    daggerComponent().inject(this);
    setSupportActionBar(toolbar);

    if (savedInstanceState == null) {
      Intent intent = getIntent();
      round = intent.getParcelableExtra(EXTRA_ROUND);
    }
    match = round.findMatchByClub(userPreferences.club());
    matchResult = match.result();
    loadClubs(match.home().id(), match.away().id());
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    stopTimer();
    MatchResultController controller = new MatchResultController(userPreferences);
    controller.updateWithResult(matchResult);
  }

  private void loadClubs(int homeId, int awayId) {
    Subscription subscription = clubObservable(homeId)
        .mergeWith(clubObservable(awayId))
        .subscribe(observer);

    subscriptions.add(subscription);
  }

  private Observable<Club> clubObservable(int id) {
    return service.club(id).compose(this.<Club>applyTransformations());
  }

  private void fillClubInfos(Club club) {
    ImageView imgView;
    TextView txtView;
    if (club.nameEquals(match.home())) {
      imgView = imgTeamHome;
      txtView = txtTeamHome;
    } else {
      imgView = imgTeamAway;
      txtView = txtTeamAway;
    }
    Picasso.with(MatchProgressActivity.this)
        .load(club.large_image())
        .into(imgView);

    txtView.setText(club.tinyName().toUpperCase());
  }

  private void stopTimer() {
    subscriptions.clear();
    isRunning = false;
  }

  private void startTimer() {
    subscriptions.add(matchResult.eventsObservable(elapsedMinutes)
        .map(matchEvent -> (Goal) matchEvent)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(goal -> {
          TextView txtScore = goal.club().nameEquals(match.home()) ? txtTeamHomeGoals : txtTeamAwayGoals;
          int currGoals = Integer.parseInt(txtScore.getText().toString());
          txtScore.setText(String.valueOf(++currGoals));
          appendEvent(goal.time() + "' " + goal.club().abbrev_name() + " goal.");
        }));

    subscriptions.add(timerObservable()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(l -> {
          elapsedMinutes++;
          fractionView.setFraction(elapsedMinutes % 45, 60);
          if (elapsedMinutes == 45) {
            appendEvent(strEndOfFirstHalf);
          } else if (elapsedMinutes == 90) {
            stopTimer();
            appendEvent(strEndOfMatch);
            appendEvent(finalScoreMessage);
            playPauseButton.setVisibility(View.GONE);
            doneButton.setVisibility(View.VISIBLE);
            fractionView.setFraction(45, 60);
          }
        }));
    isRunning = true;
  }

  @NonNull private Observable<Long> timerObservable() {
    return BuildConfig.DEBUG ? Observable.interval(0, 100, TimeUnit.MILLISECONDS)
        : Observable.interval(0, 1, TimeUnit.SECONDS);
  }

  private void appendEvent(String text) {
    txtMatchEvents.setText(text + "\n" + txtMatchEvents.getText());
  }

  @OnClick(R.id.fab_play_pause) public void onClickPause() {
    if (isRunning) {
      stopTimer();
      Snackbar.make(playPauseButton, R.string.match_paused, Snackbar.LENGTH_SHORT).show();
      playPauseButton.setImageResource(R.drawable.ic_play_arrow_white_48dp);
    } else {
      startTimer();
      Snackbar.make(playPauseButton, R.string.match_resumed, Snackbar.LENGTH_SHORT).show();
      playPauseButton.setImageResource(R.drawable.ic_pause_white_48dp);
    }
  }

  @OnClick(R.id.fab_done) public void onClickDone() {
    finish();
    startActivity(LeagueRoundResultsActivity.newIntent(this, round));
  }
}
