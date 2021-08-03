package database;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class HTMLParser {

    private Document htmlFile;

    public void setHtmlFile(String name, boolean fromWeb) {
        if (fromWeb) {
            htmlFile = loadWebPage(name);

        } else htmlFile = parseFromLocalDoc(name);
    }

    Document parseFromLocalDoc(String name) {
        try {
            return Jsoup.parse(new File(Objects.requireNonNull(HTMLParser.class.getClassLoader().
                    getResource(name)).getFile()), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    Document loadWebPage(String webPage) {
        try {
            return Jsoup.connect(webPage).timeout(2000).get();

        } catch (HttpStatusException e) {
            System.err.println(e.getMessage() +"; status = " + e.getStatusCode());
            htmlFile = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //call this method from top100_page
    Elements getLinksToAnime() {
        return htmlFile.select(".anime-column-info a[href]");
    }

    //call this method from anime_page
    String getLinkToJpg() {
        return htmlFile.select("div > .poster-block img").attr("src");
    }

    //call this method from anime_page
    String getAnimeName() {
        return htmlFile.select("div h1").text();
    }

    //call this method from anime_page
    String getAnimeStatus() {
        Elements elements = htmlFile.select("li > .badge");
        String status = elements.select("span").get(0).text();
        if (status.toLowerCase().equals("онгоинг")) status = "выходит";
        return status;
    }

    //call this method from anime_page
    String getAnimeDescription() {
        return htmlFile.select("div > .content-desc div p").text();
    }

    //call this method from anime page
    Elements getStudiousOrGenre(boolean getStudio) {
        Elements links = htmlFile.select(".content-main-info li");
        final Element[] neededElement = new Element[1];

        String studioOrGenre;
        if (getStudio) studioOrGenre = "Студия:";
        else studioOrGenre = "Жанр:";


        links.forEach(element -> {
            if (element.select("li span").text().equals(studioOrGenre)) {
                neededElement[0] = element;
            }
        });

        return neededElement[0].select("li a");
    }

    public String[] getYearAndAge() {
        String[] res = new String[2];
        Elements year = htmlFile.select(".content-main-info li");
        res[0] = year.get(2).text().split(" ")[1];
        res[1] = year.get(4).text().split(": ")[1];
        return res;
    }
}
