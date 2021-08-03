package database;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ReadTxtFile {

  public List<String> readFromFile(String fileName) {

      List<String> result = new ArrayList<>();

      File file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource(fileName)).getFile());

      try {
          FileReader fileReader = new FileReader(file);
          BufferedReader bufferedReader = new BufferedReader(fileReader);

          String line = bufferedReader.readLine();

            while (line != null) {
                result.add(line);
                line = bufferedReader.readLine();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }
}
