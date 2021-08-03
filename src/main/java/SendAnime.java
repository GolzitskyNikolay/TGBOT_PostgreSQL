import database.Anime;
import database.MyDatabase;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SendAnime {

    private final static Logger logger = Logger.getLogger(SendAnime.class);
    private AnimeBot animeBot;
    private StringBuilder userName;

    SendAnime(AnimeBot bot) {
        this.animeBot = bot;
    }

    void setUserName(StringBuilder userName){
        this.userName = userName;
    }

    void sendRandomAnime(Message message, MyDatabase database) {
        List<String> genres = database.getNamesOfAllGenres();

        Random random = new Random();
        int genreId = random.nextInt(genres.size());

        String genre = genres.get(genreId);

        logger.info("user " + userName + " choose random anime, genre = " + genre);
        sendAnimeByGenre(message, genre, database);
    }

    void sendAnimeByGenre(Message message, String genre, MyDatabase database) {

        List<Anime> animeList = database.getAnimeList(genre);
        if (animeList.size() != 0) {

            StringBuilder stringBuilder = null;

            Random random = new Random();
            int number = random.nextInt(animeList.size());

            String imageLink = null;
            Anime anime = new Anime();

            for (int i = 1; i <= animeList.size(); i++) {
                if (i - 1 == number) {
                    stringBuilder = new StringBuilder();
                    anime = animeList.get(i - 1);

                    logger.info("user " + userName + " got anime " +
                            anime.getName() + " with genre = " + genre);

                    stringBuilder.append(anime.getName().toUpperCase()).append("\n\n");

                    Integer userMark = database.getUserMarkForAnime(message.getChatId(), anime.getId());
                    if (userMark != null) {
                        stringBuilder.append("Ваша оценка: ").append(userMark).append("\n\n");
                    }

                    stringBuilder.append("СТАТУС: ").append(anime.getStatus()).append("\n\n");
                    stringBuilder.append("ГОД: ").append(anime.getYear()).append("\n\n");
                    stringBuilder.append("ВОЗРАСТНОЙ РЕЙТИНГ: ").append(anime.getAge()).append("\n\n");

                    stringBuilder.append("ЖАНРЫ: ");
                    for (int j = 1; j <= anime.getGenres().size(); j++) {
                        if (j == anime.getGenres().size()) {
                            stringBuilder.append(anime.getGenres().get(j - 1)).append("\n\n");

                        } else {
                            stringBuilder.append(anime.getGenres().get(j - 1)).append(", ");
                        }
                    }

                    stringBuilder.append("СТУДИИ: ");
                    for (int j = 1; j <= anime.getStudious().size(); j++) {
                        if (j == anime.getStudious().size()) {
                            stringBuilder.append(anime.getStudious().get(j - 1)).append("\n\n");

                        } else {
                            stringBuilder.append(anime.getStudious().get(j - 1)).append(", ");
                        }
                    }

                    stringBuilder.append("ОПИСАНИЕ: ").append(anime.getDescription()).append("\n");

                    imageLink = anime.getImage();
                    break;
                }
            }

            if (imageLink != null) {
                sendImageFromUrl(imageLink, message.getChatId().toString());
                animeBot.sendMsg(new SendMessage(), message, String.valueOf(stringBuilder));
                try {
                    animeBot.execute(showMarkAndListsButton(message.getChatId(), anime.getId()));
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }

        } else {
            animeBot.sendMsg(new SendMessage(), message, "Такого жанра нет");

        }
    }

    void sendImageFromUrl(String url, String chatId) {
        SendPhoto sendPhotoRequest = new SendPhoto();
        sendPhotoRequest.setChatId(chatId);
        sendPhotoRequest.setPhoto(url);

        try {
            animeBot.execute(sendPhotoRequest);
        } catch (TelegramApiException e) {
            logger.error("sendImageFromUrl: " + "can't load image for user " + userName + " where url = " + url);
            e.printStackTrace();
        }
    }

    SendMessage showMarkAndListsButton(long chatId, int animeId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        row.add(new InlineKeyboardButton().setText("Оценить аниме").setCallbackData("markAnime " + animeId));
        row.add(new InlineKeyboardButton().setText("Добавить в список").setCallbackData("addInList " + animeId));

        rowList.add(row);
        markup.setKeyboard(rowList);

        return new SendMessage().setChatId(chatId).setReplyMarkup(markup).setText("Вы можете оценить аниме " +
                "и добавить его в свой список");
    }
}
