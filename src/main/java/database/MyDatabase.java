package database;

import org.jsoup.select.Elements;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static database.ConstantsAndData.*;

public class MyDatabase {

    private HTMLParser htmlParser;
    private Connection connection;

    public void createDatabaseIfNotExistsAndConnect(String databaseName, String user, String password) {
        boolean databaseExists = true;

        try { //try to connect to database
            connection = DriverManager.getConnection(
                    "jdbc:postgresql://127.0.0.1:5432/" + databaseName, user, password);
            System.out.println("Database is connected");

        } catch (SQLException e) { //database doesn't exist
            System.err.println("Database doesn't exists! Creating database \"" + databaseName + "\"...");
            databaseExists = false;
        }

        if (!databaseExists) { //create database if not exists
            try {
                connection = DriverManager.getConnection( //default connection
                        "jdbc:postgresql://127.0.0.1:5432/", user, password);

                connection.prepareStatement("CREATE DATABASE " + databaseName + ";").executeUpdate();
                System.out.println("Database is created");

                connection = DriverManager.getConnection( // connected to created database
                        "jdbc:postgresql://127.0.0.1:5432/" + databaseName, user, password);
                System.out.println("Database is connected");

                createAllTables(); //create tables for empty database

                ReadTxtFile readTxtFile = new ReadTxtFile();
                List<String> links = readTxtFile.readFromFile("links"); //getting links

                fillTablesFromWeb(links);

            } catch (SQLException e) {
                System.err.format("connectToDatabase: SQL State: %s\n%s", e.getSQLState(), e.getMessage());
            }
        }
    }

    public void createAllTables() {
        try {
            createAnimeTable();
            createUserTable();
            createGenreTable();
            createStudioTable();

            createPersonalListTable();
            createListItemTable();

            createAnimeGenreTable();
            createAnimeStudioTable();

            createUserMarkTable();

        } catch (SQLException e) {
            System.err.format("createAllTables: SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        }
    }

    public void createAnimeTable() throws SQLException {
        connection.prepareStatement("CREATE TYPE status AS ENUM ('вышел', 'выходит', 'анонс');");
        connection.prepareStatement("CREATE TABLE " + ANIME +
                " (id SERIAL PRIMARY KEY, name VARCHAR(255), " +
                "status VARCHAR(7), year INTEGER, age TEXT, description TEXT, image TEXT);").executeUpdate();
    }

    public void createGenreTable() throws SQLException {
        connection.prepareStatement("CREATE TABLE " + GENRE +
                " (id SERIAL PRIMARY KEY, name VARCHAR(255));").executeUpdate();
    }

    public void createStudioTable() throws SQLException {
        connection.prepareStatement("CREATE TABLE " + STUDIO +
                " (id SERIAL PRIMARY KEY, name VARCHAR(255));").executeUpdate();
    }

    public void createUserTable() throws SQLException {
        connection.prepareStatement("CREATE TABLE " + USER +
                " (id BIGINT PRIMARY KEY, name VARCHAR(255));").executeUpdate();
    }

    public void createAnimeGenreTable() throws SQLException {
        connection.prepareStatement("CREATE TABLE " + ANIME_GENRE +
                " (anime_id INTEGER REFERENCES anime (id) ON DELETE CASCADE, " +
                "genre_id INTEGER REFERENCES genre (id) ON DELETE CASCADE);").executeUpdate();
    }

    public void createAnimeStudioTable() throws SQLException {
        connection.prepareStatement("CREATE TABLE " + ANIME_STUDIO +
                " (anime_id INTEGER REFERENCES anime (id) ON DELETE CASCADE, " +
                "studio_id INTEGER REFERENCES studio (id) ON DELETE CASCADE);").executeUpdate();
    }

    public void createPersonalListTable() throws SQLException {
        connection.prepareStatement("CREATE TABLE " + PERSONAL_LIST +
                " (id SERIAL PRIMARY KEY, name VARCHAR(255), " +
                "user_id INTEGER REFERENCES usr (id) ON DELETE CASCADE);").executeUpdate();
    }

    public void createListItemTable() throws SQLException {
        connection.prepareStatement("CREATE TABLE " + LIST_ITEM +
                " (id SERIAL PRIMARY KEY, anime_id INTEGER REFERENCES anime (id) ON DELETE CASCADE, " +
                "list_id INTEGER REFERENCES personalList (id) ON DELETE CASCADE);").executeUpdate();
    }

    public void createUserMarkTable() throws SQLException {
        connection.prepareStatement("CREATE TABLE " + USER_MARK +
                " (id SERIAL PRIMARY KEY, value INTEGER, user_id INTEGER REFERENCES usr (id) ON DELETE CASCADE, " +
                "anime_id INTEGER REFERENCES anime (id) ON DELETE CASCADE);").executeUpdate();
    }

    public void fillTablesFromWeb(List<String> otherLinks) {
        String mainPage = "https://yummyanime.club";
        Elements linksFromTop100 = null;
        htmlParser = new HTMLParser();
        int size;

        if (otherLinks.size() == 0) { //if we want to get links from top 100
            //getting links from top100 page
            htmlParser.setHtmlFile("https://yummyanime.club/top/", true);

            System.out.println("\ngetting links to anime pages");
            linksFromTop100 = htmlParser.getLinksToAnime(); // we got links from top 100
            size = linksFromTop100.size();

        } else {
            size = otherLinks.size();
        }

        for (int i = 1; i <= size; i++) {

            if (otherLinks.size() == 0) {
                //connecting to anime page from top 100
                System.out.println("link = " + mainPage + linksFromTop100.get(i - 1).attr("href") + "\n");
                htmlParser.setHtmlFile(mainPage + linksFromTop100.get(i - 1).attr("href"), true);

            } else { //connecting to anime page from other links
                System.out.println("link = " + otherLinks.get(i - 1) + "\n");
                System.out.println();
                htmlParser.setHtmlFile(otherLinks.get(i - 1), true);
            }

            try { //this is compulsory measure to receive data from the site, otherwise the site will not give data
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                System.err.println("Cant parse data from site");
            }

            int animeId = addAnimeAndReturnId();

            //adding genres of anime
            htmlParser.getStudiousOrGenre(false).forEach(genre -> {
                int genreId = addGenreOrStudioAndReturnId(genre.text().toLowerCase(), true);
                addItemInAnimeGenreOrAnimeStudioTable(animeId, genreId, true);
            });

            //adding studious of anime
            htmlParser.getStudiousOrGenre(true).forEach(studio -> {
                int studioId = addGenreOrStudioAndReturnId(studio.text().toLowerCase(), false);
                addItemInAnimeGenreOrAnimeStudioTable(animeId, studioId, false);
            });

            System.out.println("-------------------------------------------------------\n");
        }

    }

    public int addAnimeAndReturnId() {
        String name = htmlParser.getAnimeName().toLowerCase();
        String status = htmlParser.getAnimeStatus().toLowerCase();
        String[] yearAndAge = htmlParser.getYearAndAge();
        int year = Integer.parseInt(yearAndAge[0]);
        String age = yearAndAge[1];
        String description = htmlParser.getAnimeDescription();
        String image = "https://yummyanime.club" + htmlParser.getLinkToJpg();

        int animeId = -1;
        try {
            connection.prepareStatement("INSERT INTO " + ANIME + " (name, status, year, age, description, image) " +
                    "VALUES ('" + name + "', '" + status + "', " + year + ", '" + age + "', '"
                    + description + "', '" + image + "');").executeUpdate();

            System.out.println("anime \"" + name + "\" is added");

            ResultSet set1 = connection.prepareStatement("SELECT id FROM " + ANIME +
                    " WHERE name = '" + name + "';").executeQuery();

            if (set1.next()) {
                animeId = set1.getInt("id");
                System.out.println("animeId = " + animeId);
            }

        } catch (SQLException e) {
            System.out.println("Troubles with anime");
            System.err.format("addAnimeAndReturnId: SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        }

        return animeId;
    }

    public int addGenreOrStudioAndReturnId(String name, boolean isGenre) {
        int id = -1;

        String table;
        if (isGenre) table = GENRE;
        else table = STUDIO;

        try {
            ResultSet resultSet = connection.prepareStatement("SELECT id FROM " + table +
                    " WHERE name = '" + name + "';").executeQuery();

            if (resultSet.next()) {
                id = resultSet.getInt("id");
                System.out.println("table \"" + table + "\" has this " + table + ", id = " + id);

            } else {
                connection.prepareStatement("INSERT INTO " + table + " (name) VALUES ('" +
                        name + "');").executeUpdate();

                resultSet = connection.prepareStatement("SELECT id FROM " + table +
                        " WHERE name = '" + name + "';").executeQuery();

                if (resultSet.next()) {
                    id = resultSet.getInt("id");
                    System.out.println("added new " + table + " in \"" + table + "\", id = " + id);
                }
            }

        } catch (SQLException e) {
            System.out.println("Troubles with " + table);
            System.err.format("addGenreOrStudioAndReturnId: SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        }
        return id;
    }

    public void addItemInAnimeGenreOrAnimeStudioTable(int animeId, int genreOrStudioId, boolean isGenre) {

        String id;
        String table;

        if (isGenre) {
            id = "genre_id";
            table = ANIME_GENRE;
        } else {
            id = "studio_id";
            table = ANIME_STUDIO;
        }

        try {
            connection.prepareStatement("INSERT INTO " + table + " (anime_id, " + id + ") " +
                    "VALUES ('" + animeId + "', '" + genreOrStudioId + "');").executeUpdate();
            System.out.println("linked anime_id and " + id);

        } catch (SQLException e) {
            System.out.println("Troubles with linking anime_id = " + animeId +
                    " and " + id + " = " + genreOrStudioId);
            System.err.format("addItemInAnimeGenreOrAnimeStudioTable: " +
                    "SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        }
    }

    public List<String> getNamesOfPersonalListsById(long id) {
        List<String> result = new ArrayList<>();

        try {
            ResultSet set = connection.prepareStatement("SELECT * FROM " + PERSONAL_LIST +
                    " WHERE user_id = " + id + ";").executeQuery();

            while (set.next()) {
                result.add(set.getString("name"));
            }

        } catch (SQLException e) {
            System.err.format("getPersonalListsById: SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        }
        return result;
    }

    public boolean addPersonalList(long userId, String nameOfList) {
        boolean databaseHasThisName = false;

        try {
            ResultSet set = connection.prepareStatement("SELECT name FROM " + PERSONAL_LIST +
                    " WHERE user_id  = " + userId + ";").executeQuery();

            while (set.next()) {
                if (set.getString("name").toLowerCase().equals(nameOfList.toLowerCase())) {
                    databaseHasThisName = true;
                    break;
                }
            }

            if (!databaseHasThisName) {
                PreparedStatement ps = connection.prepareStatement("" +
                        "INSERT INTO " + PERSONAL_LIST + " (name, user_id) " +
                        "VALUES(?, ?);");
                ps.setString(1, nameOfList);
                ps.setLong(2, userId);
                ps.executeUpdate();

            }

        } catch (SQLException e) {
            System.err.format("addPersonalList: SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        }
        return databaseHasThisName;
    }

    public boolean addListItem(int animeId, int listId) {
        boolean wasAdded = false;

        try {
            ResultSet set = connection.prepareStatement("SELECT * FROM " + LIST_ITEM +
                    " WHERE list_id = " + listId + " AND anime_id = " + animeId + ";").executeQuery();

            if (!set.next()) {
                connection.prepareStatement("INSERT INTO " + LIST_ITEM + " (anime_id, list_id) " +
                        "VALUES(" + animeId + ", " + listId + ");").executeUpdate();
                wasAdded = true;
            }

        } catch (SQLException e) {
            System.err.format("addListItem: SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        }

        return wasAdded;

    }

    public void addOrUpdateUserMark(long userId, int userMark, int animeId) {
        try {
            ResultSet set = connection.prepareStatement("SELECT * FROM " + USER_MARK +
                    " WHERE user_id = " + userId + " AND anime_id = " + animeId + ";").executeQuery();

            if (!set.next()) {
                connection.prepareStatement("INSERT INTO " + USER_MARK + " (value, user_id, anime_id) " +
                        "VALUES(" + userMark + ", " + userId + ", " + animeId + ");").executeUpdate();
            } else {
                connection.prepareStatement("UPDATE " + USER_MARK + " SET value = " + userMark +
                        " WHERE anime_id = " + animeId + " AND user_id = " + userId + ";").executeUpdate();
            }
        } catch (SQLException e) {
            System.err.format("addOrUpdateUserMark: SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        }
    }

    public List<String> getNamesOfAllGenres() {
        List<String> genres = new ArrayList<>();
        try {
            ResultSet set = connection.prepareStatement("SELECT name FROM genre;").executeQuery();
            while (set.next()) genres.add(set.getString("name"));

        } catch (SQLException e) {
            System.err.format("getAllGenres: SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        }
        return genres;
    }

    public List<Anime> getAnimeList(String genre) {
        List<Anime> animeList = new ArrayList<>();

        try {
            ResultSet joinedTables = connection.prepareStatement(
                            "SELECT * FROM animeGenre " +
                                "JOIN anime ON anime.id = animeGenre.anime_id " +
                                "JOIN genre ON genre.id = animeGenre.genre_id " +
                                "where genre.name = '" + genre + "';").executeQuery();

            while (joinedTables.next()) {
                int id = joinedTables.getInt("anime_id");

                Anime anime = new Anime();

                anime.setId(id);
                anime.setName(joinedTables.getString("name"));
                anime.setStatus(joinedTables.getString("status"));
                anime.setYear(joinedTables.getInt("year"));
                anime.setAge(joinedTables.getString("age"));
                anime.setDescription(joinedTables.getString("description"));
                anime.setImage(joinedTables.getString("image"));
                anime.setGenres(getGenresOrStudioOfAnimeById(id, true));
                anime.setStudious(getGenresOrStudioOfAnimeById(id, false));

                animeList.add(anime);
            }
        } catch (SQLException e) {
            System.err.format("getAnimeList: SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        }
        return animeList;
    }

    public List<String> getGenresOrStudioOfAnimeById(int animeId, boolean getGenre) {
        List<String> result = new ArrayList<>();
        String id;
        String table1;
        String table2;

        if (getGenre) {
            id = "genre_id";
            table1 = ANIME_GENRE;
            table2 = GENRE;
        } else {
            id = "studio_id";
            table1 = ANIME_STUDIO;
            table2 = STUDIO;
        }

        try {
            ResultSet resultSet = connection.prepareStatement("SELECT " + id + " FROM " + table1 + " WHERE " +
                    "anime_id = " + animeId + ";").executeQuery();

            while (resultSet.next()) {
                int genreOrStudioId = resultSet.getInt(id);
                ResultSet set = connection.prepareStatement("SELECT name FROM " + table2 +
                        " WHERE id = " + genreOrStudioId + ";").executeQuery();

                if (set.next()) result.add(set.getString("name"));
            }

        } catch (SQLException e) {
            System.err.format("getGenresOrStudioOfAnimeById: SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        }
        return result;
    }

    public Integer getAnimeIdByName(String name) {
        try {
            ResultSet set = connection.prepareStatement("SELECT id FROM " + ANIME +
                    " WHERE name = '" + name + "';").executeQuery();
            if (set.next()) return set.getInt("id");

        } catch (SQLException e) {
            System.err.format("getAnimeIdByName: SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        }
        return null;
    }

    public Integer getPersonalListIdByName(String name) {
        try {
            ResultSet set = connection.prepareStatement("SELECT id FROM " + PERSONAL_LIST +
                    " WHERE name = '" + name + "';").executeQuery();
            if (set.next()) return set.getInt("id");

        } catch (SQLException e) {
            System.err.format("getPersonalListIdByName: SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        }
        return null;
    }

    // we can add user only once
    public void addUserIfNeed(String userName, long id) {
        try {
            ResultSet set = connection.prepareStatement("SELECT * FROM " + USER +
                    " WHERE id = " + id + ";").executeQuery();

            if (!set.next()) { //if database hasn't user -> add user
                System.out.println(userName + "  id = " + id);
                connection.prepareStatement("INSERT INTO " + USER + " (id, name) " +
                        "VALUES (" + id + ", '" + userName + "');").executeUpdate();
            }
        } catch (SQLException e) {
            System.err.format("addUserIfNeed: SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        }
    }

    //getting anime names from list of user
    public List<String> getAnimeByListNameAndUserId(String name, long userId) {
        List<String> animeNames = new ArrayList<>();

        try {
            ResultSet set = connection.prepareStatement("SELECT id FROM " + PERSONAL_LIST +
                    " WHERE user_id = " + userId + " AND name = '" + name + "';").executeQuery();
            Integer listId = null;
            if (set.next()) listId = set.getInt("id");


            List<Integer> animeIdentifiers = new ArrayList<>();
            set = connection.prepareStatement("SELECT anime_id FROM " + LIST_ITEM +
                    " WHERE list_id = " + listId + ";").executeQuery();
            while (set.next()) {
                animeIdentifiers.add(set.getInt("anime_id"));
            }


            for (int i = 1; i <= animeIdentifiers.size(); i++) {
                set = connection.prepareStatement("SELECT name FROM " + ANIME +
                        " WHERE id = " + animeIdentifiers.get(i - 1) + ";").executeQuery();
                if (set.next()) animeNames.add(set.getString("name"));
            }

        } catch (SQLException e) {
            System.err.format("getAnimeByListNameAndUserId: SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        }
        return animeNames;
    }

    public void deletePersonalList(String name, long userId) {
        try {
            connection.prepareStatement("DELETE FROM " + PERSONAL_LIST +
                    " WHERE name = '" + name + "' AND user_id = " + userId + ";").executeUpdate();
        } catch (SQLException e) {
            System.err.format("deletePersonalList: SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        }
    }

    public Integer getUserMarkForAnime(long userId, int animeId) {
        try {
            ResultSet set = connection.prepareStatement("SELECT value FROM " + USER_MARK +
                    " WHERE user_id = " + userId + " AND anime_id = " + animeId + ";").executeQuery();

            if (set.next()) return set.getInt("value");
        } catch (SQLException e) {
            System.err.format("getUserMarkForAnime: SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        }

        return null;
    }

    public void deleteAnimeFromList(int animeId, int listId) {
        try {
            connection.prepareStatement("DELETE FROM " + LIST_ITEM + " WHERE anime_id = " + animeId +
                    " AND list_id = " + listId + ";").executeQuery();
        } catch (SQLException e) {
            System.err.format("deleteAnimeFromList: SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        }
    }
}