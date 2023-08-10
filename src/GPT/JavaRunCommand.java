package GPT;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class JavaRunCommand {
    public static void main(String[] args) {
        try {

            // venv with OpenAI lib 
            String pythonInterpreter = "C:\\Users\\katar\\OneDrive\\Radna površina\\VS-InspectorGPT\\venvOpenAI\\Scripts\\python.exe";

            // execute python script
            ProcessBuilder processBuilder = new ProcessBuilder(pythonInterpreter, "src\\GPT\\callGPT.py");
            Process process = processBuilder.start();

            // Get the input stream from the process
            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            // Wait for the process to finish
            int exitCode = process.waitFor();
            System.out.println("Python script finished with exit code: " + exitCode);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String ask(String question){


        try {

            // venv with OpenAI lib 
            String pythonInterpreter = "C:\\Users\\katar\\OneDrive\\Radna površina\\VS-InspectorGPT\\venvOpenAI\\Scripts\\python.exe";

            // execute python script
            ProcessBuilder processBuilder = new ProcessBuilder(pythonInterpreter, "src\\GPT\\callGPT.py", question);
            Process process = processBuilder.start();

            // Get the input stream from the process
            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            String output="";
            while ((line = reader.readLine()) != null) {
                //System.out.println(line);
                output += line;
            }

            // Wait for the process to finish
            int exitCode = process.waitFor();
            //System.out.println("Python script finished with exit code: " + exitCode);

            return output;

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }


        return null;
    }

    
}
