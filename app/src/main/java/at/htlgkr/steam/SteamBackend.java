package at.htlgkr.steam;

import android.annotation.SuppressLint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SteamBackend {
    @SuppressLint("SimpleDateFormat") private SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Game.DATE_FORMAT);
    private List<Game> games;
    public SteamBackend() {
        games = new ArrayList<>();
    }

    public void loadGames(InputStream inputStream) {
        games.clear();

        try(BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))){
            String currentLine = reader.readLine();
            while(currentLine != null) {
                String[] arguments = currentLine.split(";");
                try {
                    games.add(new Game(arguments[0], simpleDateFormat.parse(arguments[1]), Double.parseDouble(arguments[2])));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                currentLine = reader.readLine();
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public void store(OutputStream fileOutputStream) {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(fileOutputStream));
        games.forEach(currentGame ->
                writer.println(currentGame.getName() + ";" + simpleDateFormat.format(currentGame.getReleaseDate()) + ";" + currentGame.getPrice()));
        writer.flush();
        writer.close();
    }

    public List<Game> getGames() {
        return this.games;
    }

    public void setGames(List<Game> games) {
        this.games = games;
    }

    public void addGame(Game newGame) {
        this.games.add(newGame);
    }

    public double sumGamePrices() {
        return games.stream().mapToDouble(Game::getPrice).sum();
    }

    public double averageGamePrice() {
        return games.stream().mapToDouble(Game::getPrice).average().orElse(0.0);
    }

    public List<Game> getUniqueGames() {
        return games.stream().distinct().collect(Collectors.toList());
    }

    public List<Game> selectTopNGamesDependingOnPrice(int n) {
        return games.stream()
                .sorted((game, game2) -> Double.compare(game2.getPrice(), game.getPrice()))
                .limit(n)
                .collect(Collectors.toList());
    }
}
