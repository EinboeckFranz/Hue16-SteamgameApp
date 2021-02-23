package at.htlgkr.steamgameapp;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import at.htlgkr.steam.Game;
import at.htlgkr.steam.ReportType;
import at.htlgkr.steam.SteamBackend;

public class MainActivity extends AppCompatActivity {
    private static final String GAMES_CSV = "games.csv";
    private final SteamBackend backend = new SteamBackend();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadGamesIntoListView();
        setUpReportSelection();
        setUpSearchButton();
        setUpAddGameButton();
        setUpSaveButton();
    }

    private void loadGamesIntoListView() {
        try {
            backend.loadGames(getAssets().open(GAMES_CSV));
            ListView gamesList = findViewById(R.id.gamesList);
            gamesList.setAdapter(new GameAdapter(this, R.layout.game_item_layout, backend.getGames()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setUpReportSelection() {
        List<ReportTypeSpinnerItem> spinnerItemList = new ArrayList<>();

        spinnerItemList.add(new ReportTypeSpinnerItem(ReportType.NONE,SteamGameAppConstants.SELECT_ONE_SPINNER_TEXT));
        spinnerItemList.add(new ReportTypeSpinnerItem(ReportType.SUM_GAME_PRICES,SteamGameAppConstants.SUM_GAME_PRICES_SPINNER_TEXT));
        spinnerItemList.add(new ReportTypeSpinnerItem(ReportType.AVERAGE_GAME_PRICES,SteamGameAppConstants.AVERAGE_GAME_PRICES_SPINNER_TEXT));
        spinnerItemList.add(new ReportTypeSpinnerItem(ReportType.UNIQUE_GAMES,SteamGameAppConstants.UNIQUE_GAMES_SPINNER_TEXT));
        spinnerItemList.add(new ReportTypeSpinnerItem(ReportType.MOST_EXPENSIVE_GAMES,SteamGameAppConstants.MOST_EXPENSIVE_GAMES_SPINNER_TEXT));

        Spinner reportTypeSpinner = findViewById(R.id.chooseReport);
        reportTypeSpinner.setAdapter(new ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, spinnerItemList));
        reportTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ReportTypeSpinnerItem spinnerItem = (ReportTypeSpinnerItem) adapterView.getItemAtPosition(i);

                AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                alert.setTitle(spinnerItem.getDisplayText());

                switch(spinnerItem.getType()) {
                    case SUM_GAME_PRICES:
                        alert.setMessage(SteamGameAppConstants.ALL_PRICES_SUM + backend.sumGamePrices()).show();
                        break;
                    case AVERAGE_GAME_PRICES:
                        alert.setMessage(SteamGameAppConstants.ALL_PRICES_AVERAGE + backend.averageGamePrice()).show();
                        break;
                    case UNIQUE_GAMES:
                        alert.setMessage(SteamGameAppConstants.UNIQUE_GAMES_COUNT + backend.getUniqueGames().size()).show();
                        break;
                    case MOST_EXPENSIVE_GAMES:
                        List<Game> topGames = backend.selectTopNGamesDependingOnPrice(3);
                        alert.setMessage(SteamGameAppConstants.MOST_EXPENSIVE_GAMES +
                                "\n" + topGames.get(0) +
                                "\n" + topGames.get(1) +
                                "\n" + topGames.get(2)
                        ).show();
                        break;
                    case NONE:
                    default:
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                //DO NOTHING :)
            }
        });
    }

    private void setUpSearchButton() {
        Button searchBtn = findViewById(R.id.search);
        searchBtn.setOnClickListener(view -> {
            EditText searchTerm = new EditText(getApplicationContext());
            searchTerm.setId(R.id.dialog_search_field);

            AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
            alert.setTitle(SteamGameAppConstants.ENTER_SEARCH_TERM)
                .setView(searchTerm)
                .setPositiveButton("Confirm", (dialogInterface, i) -> {
                    List<Game> filteredGames = backend.getGames().stream()
                            .filter(game -> game.getName().toLowerCase().trim().contains(
                                            searchTerm.getText().toString().toLowerCase().trim())
                            ).collect(Collectors.toList());
                    ListView gamesList = findViewById(R.id.gamesList);
                    gamesList.setAdapter(new GameAdapter(getApplicationContext(),R.layout.game_item_layout,filteredGames));
                }).setNegativeButton("Cancel", null)
                .show();
        });
    }

    private void setUpAddGameButton() {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat(Game.DATE_FORMAT);
        Button addBtn = findViewById(R.id.addGame);
        addBtn.setOnClickListener(view -> {
            LinearLayout linearLayout = new LinearLayout(getApplicationContext());
            linearLayout.setOrientation(LinearLayout.VERTICAL);

            EditText gameName = new EditText(getApplicationContext());
            gameName.setId(R.id.dialog_name_field);
            linearLayout.addView(gameName);

            EditText gameReleaseDate = new EditText(getApplicationContext());
            gameReleaseDate.setId(R.id.dialog_date_field);
            linearLayout.addView(gameReleaseDate);

            EditText gamePrice = new EditText(getApplicationContext());
            gamePrice.setId(R.id.dialog_price_field);
            linearLayout.addView(gamePrice);

            AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
            alert.setTitle(SteamGameAppConstants.NEW_GAME_DIALOG_TITLE)
                .setView(linearLayout)
                .setPositiveButton("Confirm", (dialogInterface, i) -> {
                    try {
                        backend.addGame(new Game(
                                gameName.getText().toString(),
                                sdf.parse(gameReleaseDate.getText().toString()),
                                Double.parseDouble(gamePrice.getText().toString())
                        ));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }).setNegativeButton("Cancel", null)
                .show();
        });
    }

    private void setUpSaveButton() {
        Button saveBtn = findViewById(R.id.save);
        saveBtn.setOnClickListener(view -> {
            try {
                backend.store(openFileOutput(SteamGameAppConstants.SAVE_GAMES_FILENAME, MODE_PRIVATE));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        });
    }
}
