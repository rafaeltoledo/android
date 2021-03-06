package com.felipecsl.elifut.preferences;

import android.content.SharedPreferences;

import com.f2prateek.rx.preferences.Preference;
import com.f2prateek.rx.preferences.RxSharedPreferences;
import com.felipecsl.elifut.models.Club;
import com.felipecsl.elifut.models.League;
import com.felipecsl.elifut.models.Nation;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

public final class UserPreferences {
  private static final String KEY_USER_CLUB = "UserClub";
  private static final String KEY_USER_NATION = "UserNation";
  private static final String KEY_USER_LEAGUE = "UserLeague";
  private static final String KEY_COACH_NAME = "COACH_NAME";

  private final JsonPreference<Nation> nationPreference;
  private final JsonPreference<Club> clubPreference;
  private final JsonPreference<League> leaguePreference;
  private final Preference<String> coachPreference;

  public UserPreferences(SharedPreferences preferences, Moshi moshi) {
    RxSharedPreferences rxSharedPreferences = RxSharedPreferences.create(preferences);
    JsonAdapter<Nation> nationAdapter = moshi.adapter(Nation.class);
    JsonAdapter<Club> clubAdapter = moshi.adapter(Club.class);
    JsonAdapter<League> leagueAdapter = moshi.adapter(League.class);
    nationPreference = new JsonPreference<>(rxSharedPreferences, nationAdapter, KEY_USER_NATION);
    clubPreference = new JsonPreference<>(rxSharedPreferences, clubAdapter, KEY_USER_CLUB);
    leaguePreference = new JsonPreference<>(rxSharedPreferences, leagueAdapter, KEY_USER_LEAGUE);
    coachPreference =rxSharedPreferences.getString(KEY_COACH_NAME);
  }

  public Club club() {
    return clubPreference.get();
  }

  public Nation nation() {
    return nationPreference.get();
  }

  public League league() {
    return leaguePreference.get();
  }

  public String coach() {
    return coachPreference.get();
  }

  public JsonPreference<Nation> nationPreference() {
    return nationPreference;
  }

  public JsonPreference<Club> clubPreference() {
    return clubPreference;
  }

  public JsonPreference<League> leaguePreference() {
    return leaguePreference;
  }

  public Preference<String> coachPreference() {
    return coachPreference;
  }
}
