package com.felipecsl.elifut.adapter;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.felipecsl.elifut.R;
import com.felipecsl.elifut.activitiy.TeamDetailsActivity;
import com.felipecsl.elifut.adapter.LeagueMatchesAdapter.ItemViewHolder;
import com.felipecsl.elifut.models.Club;
import com.felipecsl.elifut.models.LeagueRound;
import com.felipecsl.elifut.models.Match;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.BindColor;
import butterknife.ButterKnife;

import static com.google.common.base.Preconditions.checkNotNull;

public final class LeagueMatchesAdapter
    extends RecyclerViewHeaderListAdapter<Match, String, ItemViewHolder, SimpleHeaderViewHolder> {
  private final Club currentClub;

  public LeagueMatchesAdapter(Club currentClub, String headerText, List<Match> matches) {
    super(new ArrayList<>(matches), checkNotNull(headerText));
    this.currentClub = checkNotNull(currentClub);
  }

  @Override protected BaseViewHolder.Factory<SimpleHeaderViewHolder> headerFactory() {
    return (parent, viewType) ->
        new SimpleHeaderViewHolder(parent, R.layout.adapter_round_header_item);
  }

  @Override protected BaseViewHolder.Factory<ItemViewHolder> itemFactory() {
    return (parent, viewType) -> new ItemViewHolder(parent);
  }

  public void setItems(LeagueRound round) {
    setData(new ArrayList<>(round.matches()));
  }

  class ItemViewHolder extends BaseViewHolder<Match> {
    @Bind(R.id.outer_layout) FrameLayout layoutOuter;
    @Bind(R.id.layout_team_home) LinearLayout layoutTeamHome;
    @Bind(R.id.layout_team_away) LinearLayout layoutTeamAway;
    @Bind(R.id.img_team_home) ImageView imgTeamHome;
    @Bind(R.id.img_team_away) ImageView imgTeamAway;
    @Bind(R.id.txt_team_home) TextView txtTeamHome;
    @Bind(R.id.txt_team_away) TextView txtTeamAway;

    @BindColor(R.color.light_gray) int colorLightGray;
    @BindColor(android.R.color.transparent) int colorTransparent;
    @BindColor(R.color.material_red_300) int colorCurrentTeam;

    ItemViewHolder(ViewGroup parent) {
      super(parent, R.layout.adapter_match_result_item);
      ButterKnife.bind(this, itemView);
    }

    @Override public void bind(Match match) {
      Club home = match.home();
      Club away = match.away();

      Picasso.with(itemView.getContext())
          .load(home.large_image())
          .into(imgTeamHome);

      Picasso.with(itemView.getContext())
          .load(away.large_image())
          .into(imgTeamAway);

      txtTeamHome.setText(home.tinyName().toUpperCase());
      txtTeamAway.setText(away.tinyName().toUpperCase());
      Context context = itemView.getContext();
      layoutTeamAway.setOnClickListener(view -> context.startActivity(
          TeamDetailsActivity.newIntent(context, away)));
      layoutTeamHome.setOnClickListener(view -> context.startActivity(
          TeamDetailsActivity.newIntent(context, home)));

      if (!match.hasClub(currentClub)) {
        layoutOuter.setBackgroundColor(
            getAdapterPosition() % 2 != 0 ? colorLightGray : colorTransparent);
      } else {
        layoutOuter.setBackgroundColor(colorCurrentTeam);
      }
    }
  }
}
