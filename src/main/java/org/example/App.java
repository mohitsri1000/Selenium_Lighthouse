package org.example;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.*;
import java.time.Duration;

public class App {
    public static void main(String[] args) throws IOException {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("debuggerAddress", "127.0.0.1:9222");

        chromeDebug();

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        driver.navigate().to("https://web.dev/");
        WebElement logo = driver.findElement(By.cssSelector("img[alt=\"web.dev\"]"));
        wait.until(ExpectedConditions.visibilityOf(logo));

        String url = driver.getCurrentUrl();
        String newWindow = windowOperations(driver);

        Process timeSpanProcess = startTimespan(url, "web-dev");

        driver.switchTo().window(driver.getWindowHandles().stream().filter(window -> !window.equals(newWindow)).findFirst().orElse(newWindow));

        WebElement searchBox = driver.findElement(By.cssSelector("input[aria-label=\"Search\"]"));
        searchBox.click();
        searchBox.sendKeys("CLS" + Keys.ENTER);

        By searchResultsLocator = By.cssSelector("devsite-content a[href=\"https://web.dev/articles/cls\"]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(searchResultsLocator));

        endTimespan(timeSpanProcess);

        driver.close();
        driver.switchTo().window(newWindow);
        driver.navigate().to(url);
        driver.close();
        driver.quit();
    }

    private static String windowOperations(WebDriver driver) {
        String originalWindow = driver.getWindowHandle();
        driver.switchTo().newWindow(WindowType.TAB);
        String newWindow = driver.getWindowHandle();
        driver.switchTo().window(originalWindow);
        driver.close();
        driver.switchTo().window(newWindow);
        return newWindow;
    }

    private static void chromeDebug() throws IOException {
        ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", "chrome-debug --port=9222");
        builder.redirectErrorStream(true);
        builder.start();
    }

    private static Process startTimespan(String url, String reportName) throws IOException {
        ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", "node", "selenium-run.js", "--url=" + url, "--reportName=" + reportName, "--timeout=1800000");
        System.out.println(builder.toString());
        builder.redirectErrorStream(true);

        Process process = builder.start();

        BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while (true) {

            line = r.readLine();
            if (line == null || line.equals("Write FINISH to end Timespan:")) {
                break;
            }
            System.out.println(line);
        }

        return process;
    }

    private static void endTimespan(Process process) throws IOException {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));
        writer.println("FINISH");
        writer.flush();

        BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;

        while (true) {

            line = r.readLine();
            if (line == null) {
                break;
            }
            System.out.println(line);
        }
    }
}
