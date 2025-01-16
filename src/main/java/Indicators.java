import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import io.github.bonigarcia.wdm.WebDriverManager;

import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Indicators {
    private static final String RESOURCE_FILE_PATH = "AAFC SNS 경로.txt";
    private static final String RESULT_FILE_PATH = "결과.txt";

    public static void main(String[] args) throws IOException {

        Indicators indicators = new Indicators();

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();

        ChromeDriver driver = new ChromeDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        Path path = Paths.get(RESULT_FILE_PATH);
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(path.toAbsolutePath().toString(), false), StandardCharsets.UTF_8));

        writer.println(String.format("%-8s %-8s %-8s %-8s %-8s %-8s", "지점명", "당월 게시글", "팔로워", "좋아요_평균", "댓글_평균", "참여도"));

        List<String> pointList = indicators.ListRead();

        indicators.InstagramLogin(driver, wait);

        for (String point : pointList) {
            indicators.WritePointInfo(point, driver, wait, writer);
        }
        writer.close();
        driver.close();
    }

    private void InstagramLogin(ChromeDriver driver, WebDriverWait wait) {
        // 로그인
        String baseUrl = "https://www.instagram.com/accounts/login/";
        driver.get(baseUrl);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("username")));
        driver.findElement(By.name("username")).sendKeys("aafc.co.kr");
        driver.findElement(By.name("password")).sendKeys("aafc1208");
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button._acan._acap._acas._aj1-._ap30")));
        btn.click();
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[text()=\"나중에 하기\"]"))).click();
        } catch (NoSuchElementException ignored) {
        }
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("/html/body/div[2]/div[1]/div/div[2]/div/div/div/div/div[2]/div/div/div[3]/button[2]"))).click();
        } catch (Exception ignored) {
        }
    }

    private void WritePointInfo(String point, ChromeDriver driver, WebDriverWait wait, PrintWriter writer) {
        String[] pointSplit = point.split(" ");
        // 프로필 정보 가져오기
        driver.get(pointSplit[1]);

        int[] iterateBoards = IterateBoards(driver, wait);
        PointInfo pointInfo = new PointInfo.PointInfoBuilder()
                .pointName(pointSplit[0])
                .monthlyPosts(iterateBoards[2])
                .likesAve(iterateBoards[0])
                .commentsAve(iterateBoards[1])
                .followers(iterateBoards[3])
                .build();
        writer.println(pointInfo);
    }

    private int[] IterateBoards(ChromeDriver driver, WebDriverWait wait) {
        // 동적 화면 구성에 의한 화면 크기 조정
        driver.manage().window().setSize(new Dimension(800, 600));
        Actions actions = new Actions(driver);
        int[] result = {0, 0, 0, 0};
        int i = 1;

        try {
            result[3] = Integer.parseInt(
                    wait.until(
                            ExpectedConditions.visibilityOfElementLocated(
                                    By.xpath("//div/div/div[2]/div/div/div[1]/div[2]/div/div[1]/section/main/div/header/section[3]/ul/li[2]/div/a/span/span"))
                    ).getText()
            );
            // 게시글 순회
            while (i > 0) {
                Thread.sleep(3000);
                for (int j = 1; j <= 3; j++) {
                    // 게시물 선택
                    WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(String.format("//div/div/div[2]/div/div/div[1]/div[2]/div/div[1]/section/main/div/div[2]/div/div[%d]/div[%d]/a", i, j))));
                    // 현재 게시물이 고정 게시물일 경우 다음 게시물로 이동
                    try {
                        WebElement element1 = driver.findElement(By.xpath(String.format("//div/div/div[2]/div/div/div[1]/div[2]/div/div[1]/section/main/div/div[2]/div/div[%d]/div[%d]/a//*[local-name()='svg' and @aria-label=\"고정 게시물\"]", i, j)));
                        if (element1.isDisplayed()) {
                            continue;
                        }
                    } catch (NoSuchElementException ignored) {
                    }
                    // 선택한 게시물 열람
                    element.click();
                    // 게시물의 게시 날짜 확인 시 당월 게시물이 아닌 경우 게시글 순회 탈출
                    String timeString = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("time.x1p4m5qa"))).getAttribute("title");
                    if (!CheckDate(timeString)) {
                        i = -1;
                        break;
                    }
                    // 게시물 나가기
                    WebElement until = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.x160vmok.x10l6tqk.x1eu8d0j.x1vjfegm")));
                    until.click();
                    // 해당 게시물의 좋아요, 댓글 수를 알기 위해 마우스 포인터 올리는 작업
                    actions.moveToElement(element).perform();
                    // 인스타 게시물의 index는 최대 12까지 있으므로 12번째 게시물에 도달할 경우 무한 순회를 방지하기 위해 순회 순서를 하나 뺀다.
                    if (i > 11) {
                        i--;
                    }
                    result[0] += Integer.parseInt(wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(String.format("//div/div/div[2]/div/div/div[1]/div[2]/div/div[1]/section/main/div/div[2]/div/div[%d]/div[%d]/a/div[3]/ul/li[1]/span[1]/span", i, j)))).getText());
                    result[1] += Integer.parseInt(wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(String.format("//div/div/div[2]/div/div/div[1]/div[2]/div/div[1]/section/main/div/div[2]/div/div[%d]/div[%d]/a/div[3]/ul/li[2]/span[1]/span", i, j)))).getText());
                    result[2]++;
                }
                i++;
            }
        } catch (TimeoutException | InterruptedException ignored) {
        }
        return result;
    }

    // 지점 리스트 읽기
    private List<String> ListRead() throws IOException {
        String line;
        Path path = Paths.get(RESOURCE_FILE_PATH);
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path.toAbsolutePath().toString()), StandardCharsets.UTF_8));
        ArrayList<String> resultList = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            resultList.add(line);
        }
        return resultList;
    }

    // 당월과 게시글 게시 월 비교
    private boolean CheckDate(String dateStr) {
        // 문자열 날짜 변환
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일");
        LocalDate postDate = LocalDate.parse(dateStr, formatter);

        // 현재 날짜
        LocalDate now = LocalDate.now();

        return postDate.getMonthValue() == now.getMonthValue() && postDate.getYear() == now.getYear();
    }
}