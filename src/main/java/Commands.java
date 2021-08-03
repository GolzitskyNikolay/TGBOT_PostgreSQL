import database.MyDatabase;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Commands {
    private final Logger logger = Logger.getLogger(Commands.class);
    private AnimeBot animeBot;
    String lastCallbackText = null;
    private StringBuilder userName;

    Commands(AnimeBot animeBot) {
        this.animeBot = animeBot;
    }

    void setUserName(StringBuilder userName){
        this.userName = userName;
    }

    void help(Message message) {
        logger.info("/help command");

        String helpText = "Поддерживаемые комманды:\n \"/genre\" рекомендует аниме по жанру;\n" +
                "\"/random\" рекоммендует случайное аниме;\n \"/lists\" показывает ваши списки аниме, " +
                "если они созданы\n ";

        SendMessage sendMessage = new SendMessage();
        showBasicCommands(sendMessage);
        animeBot.sendMsg(sendMessage, message, helpText);
    }

    void start(Message message) {
        logger.info("/start command");

        String startText = "Привет! Моя задача - рекомендовать аниме. Просто отправь мне жанр аниме," +
                " а я пришлю тебе популярные аниме этого жанра.\nДля этого используй команду \"/genre\"\n" +
                "Или можешь использовать команду \"/random\" чтобы получить случайное аниме;\n" +
                "\"/lists\" показывает ваши списки аниме, если они созданы";

        SendMessage sendMessage = new SendMessage();
        setCommandButton(sendMessage);

        animeBot.sendMsg(sendMessage, message, startText);
    }

    void listCommands(Message message, MyDatabase database) {
        logger.info("/lists command");

        SendMessage sendMessage = new SendMessage();
        showListCommands(sendMessage);
        animeBot.sendMsg(sendMessage, message, "\"/createlist\" создаёт новый список," +
                " \"/deletelist\" удаляет список");

        try {
            animeBot.execute(showLists(message.getChatId(),
                    "listNameToOpen = ", "Ваши списки:", database));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    void showBasicCommands(SendMessage sendMessage) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        sendMessage.setReplyMarkup(replyKeyboardMarkup);

        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow keyboardRow1 = new KeyboardRow();
        keyboardRow1.add(new KeyboardButton("/genre"));
        keyboardRow1.add(new KeyboardButton("/random"));
        keyboardRow1.add((new KeyboardButton("/lists")));

        keyboard.add(keyboardRow1);

        replyKeyboardMarkup.setKeyboard(keyboard);
    }

    //buttons in keyboard panel
    public synchronized void setCommandButton(SendMessage sendMessage) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        sendMessage.setReplyMarkup(replyKeyboardMarkup);

        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow keyboardRow1 = new KeyboardRow();
        keyboardRow1.add(new KeyboardButton("/help"));

        keyboard.add(keyboardRow1);

        replyKeyboardMarkup.setKeyboard(keyboard);
    }

    private synchronized void showListCommands(SendMessage sendMessage) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        sendMessage.setReplyMarkup(replyKeyboardMarkup);

        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow keyboardRow1 = new KeyboardRow();
        keyboardRow1.add(new KeyboardButton("/createlist"));
        keyboardRow1.add(new KeyboardButton("/deletelist"));
        keyboardRow1.add((new KeyboardButton("/lists")));

        KeyboardRow keyboardRow2 = new KeyboardRow();
        keyboardRow2.add(new KeyboardButton("/help"));

        keyboard.add(keyboardRow1);
        keyboard.add(keyboardRow2);

        ReplyKeyboardRemove replyKeyboardRemove = new ReplyKeyboardRemove();
        replyKeyboardRemove.getRemoveKeyboard();
        replyKeyboardMarkup.setKeyboard(keyboard);
    }

    SendMessage showLists(long chatId, String callBack, String textInMessage, MyDatabase database) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        List<String> names = database.getNamesOfPersonalListsById(chatId);

        if (names.size() != 0) {
            Collections.sort(names);

            logger.info("showLists: user " + userName + " opened lists, count of lists = " + names.size());

            for (int i = 1; i <= names.size(); i++) {
                String listName = names.get(i - 1);
                row.add(new InlineKeyboardButton().setText(listName).setCallbackData(callBack + listName));

                if (i % 2 == 0) {
                    rowList.add(row);
                    row = new ArrayList<>();
                }

                if (i % 2 == 1 && i == names.size()) {
                    rowList.add(row);
                }
            }

            markup.setKeyboard(rowList);
            return new SendMessage().setChatId(chatId).setText(textInMessage).setReplyMarkup(markup);

        } else {
            logger.info("showLists: user " + userName + " hasn't lists");

            String hint = "У вас пока нет списков. Чтобы добавить список используйте команду /createlist";
            return new SendMessage().setChatId(chatId).setText(hint).setReplyMarkup(markup);
        }
    }

    //buttons under message
    public SendMessage showGenres(Message message, MyDatabase database) {
        long chatId = message.getChatId();
        logger.info("show genres for user " + userName);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        List<String> genres = database.getNamesOfAllGenres();
        Collections.sort(genres);

        int currentLength = 0;
        int maxSize = 25;

        for (int i = 1; i <= genres.size(); i++) {
            String genre = genres.get(i - 1);
            currentLength += genre.length();

            if (currentLength > maxSize) {
                currentLength = genre.length();
                rowList.add(row);
                row = new ArrayList<>();
            }

            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(genre).setCallbackData("genre = " + genre);

            lastCallbackText = "genre = " + genre;
            row.add(button);
        }

        markup.setKeyboard(rowList);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setReplyToMessageId(message.getMessageId()).
                setChatId(chatId).setText("Выберите жанр:").setReplyMarkup(markup);

        return sendMessage;
    }

    void createList(String text, Message message, MyDatabase database) {
        animeBot.setLastCommand(null);
        StringBuilder normalName = new StringBuilder();
        String[] string = text.split("[ |\t]+");
        List<String> onlyWords = new ArrayList<>();

        for (int i = 1; i <= string.length; i++) {
            if (!string[i - 1].equals("")) onlyWords.add(string[i - 1]);
        }

        for (int i = 1; i <= onlyWords.size(); i++) {
            normalName.append(onlyWords.get(i - 1));
            if (i != onlyWords.size()) normalName.append(" ");
        }

        String name = normalName.toString();
        logger.info("user " + userName + " is trying to create list with name \"" + name + "\"");

        if (name.matches(" *")) {
            logger.info("user " + userName + "tried to create empty list");
            animeBot.sendMsg(new SendMessage(), message, "Название списка не должно быть пустым!");

        } else if (name.matches("[а-яА-Я|ё|Ё|a-zA-Z0-9|\\-| ,;?!.]+")){
            boolean hasThisName = database.addPersonalList(message.getChatId(), name);

            if (!hasThisName) {
                animeBot.sendMsg(new SendMessage(), message, "Список " + name + " создан");
                logger.info("user " + userName + " created list");

            } else {
                animeBot.sendMsg(new SendMessage(), message, "Такой список у вас уже есть");
                logger.info("user " + userName + " already has list with this name");
            }
        } else {
            animeBot.sendMsg(new SendMessage(), message,
                    "Название списка не должно содержать специальных символов! " +
                            "Используйте только буквенные и численные значения!");
            logger.info("user " + userName + " didn't create list, cause used special symbols");
        }
    }

    void deleteAnimeFromList(String text, Message message, MyDatabase database) {
        String listName = animeBot.getLastCommand().split("/deleteAnime from ")[1];
        logger.info("user " + userName + " is trying to delete anime from list \"" + listName + "\"");

        if (listName == null) {
            logger.warn("user " + userName + " tried to open null-list, listName = null");
            animeBot.sendMsg(new SendMessage(), message,
                    "Откройте список, из которого вы хотите удалить аниме!");
        } else {
            animeBot.setLastCommand(null);

            if (!text.matches("[ |\t]*[1-9]+[0-9]*[ |\t]*")) {
                logger.warn("user " + userName + "tried to delete anime with incorrect number = " + text);
                animeBot.sendMsg(new SendMessage(), message, "Номер введён некорректно");

            } else {
                int numberInList = Integer.parseInt(text);

                List<String> animeNames = database.getAnimeByListNameAndUserId(listName, message.getChatId());
                Collections.sort(animeNames);

                Integer listId = database.getPersonalListIdByName(listName);

                if (listId == null) {
                    logger.warn("user " + userName + " tried to open null-list, listId = null");
                    animeBot.sendMsg(new SendMessage(), message, "Откройте список, " +
                            "из которого вы хотите удалить аниме!");

                } else if (animeNames.size() < numberInList) {
                    logger.warn("user " + userName + " tried to delete anime," +
                            " but list hasn't this number, number = " + numberInList);
                    animeBot.sendMsg(new SendMessage(), message, "Такого номера в списке нет!");

                } else {
                    int animeId = database.getAnimeIdByName(animeNames.get(numberInList - 1));

                    database.deleteAnimeFromList(animeId, listId);
                    SendMessage sendMessage = new SendMessage();
                    showBasicCommands(sendMessage);
                    animeBot.sendMsg(sendMessage, message, "Аниме \"" + animeNames.get(numberInList - 1) +
                            "\" удалено из списка \"" + listName + "\"");

                    logger.info("anime \"" + animeNames.get(numberInList - 1) + "\" was deleted from list \""
                            + listName + "\" of user " + userName);
                }
            }
        }
    }
}