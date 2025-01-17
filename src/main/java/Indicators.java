import java.io.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.bonigarcia.wdm.WebDriverManager;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Indicators {
    private static final String RESOURCE_FILE_PATH = "AAFC SNS 경로.xlsx";
    private static final String[] POINT_INFO = {"지점명", "당월 게시글", "팔로워", "좋아요_평균", "댓글_평균", "참여도"};

    public static void main(String[] args) throws IOException {

        Indicators indicators = new Indicators();

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
//        options.addArguments("--headless");
        options.addArguments("--disable-blink-features=AutomationControlled");

        ChromeDriver driver = new ChromeDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Indicator");
        Row row = sheet.createRow(0);

        for (int i = 0; i < POINT_INFO.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(POINT_INFO[i]);
        }

        LinkedHashMap<String,String> pointList = indicators.ListRead();

        indicators.InstagramLogin(driver, wait);

        AtomicInteger rowIndex = new AtomicInteger(0);
        pointList.forEach((key,value)->indicators.WritePointInfo(key, value, driver, wait, sheet.createRow(rowIndex.incrementAndGet())));

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

        try (FileOutputStream file = new FileOutputStream(now.format(formatter) + "-결과.xlsx")) {
            workbook.write(file);
        }

        workbook.close();
        driver.quit();
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

    private void WritePointInfo(String pointName, String pointUrl, ChromeDriver driver, WebDriverWait wait, Row row) {
        // 프로필 정보 가져오기
        driver.get(pointUrl);

        int[] iterateBoards = IterateBoards(driver, wait);
        String[] pointInfo = new PointInfo.PointInfoBuilder()
                .pointName(pointName)
                .monthlyPosts(iterateBoards[2])
                .likesAve(iterateBoards[0])
                .commentsAve(iterateBoards[1])
                .followers(iterateBoards[3])
                .build()
                .toArray();

        for (int i = 0; i < pointInfo.length; i++) {
            row.createCell(i).setCellValue(pointInfo[i]);
        }
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
        } catch (TimeoutException ignored) {
        }
        return result;
    }

    // 지점 리스트 읽기
    private LinkedHashMap<String, String> ListRead() throws IOException {
        FileInputStream fileInputStream = new FileInputStream(RESOURCE_FILE_PATH);
        Workbook workbook = new XSSFWorkbook(fileInputStream);
        Sheet sheet = workbook.getSheetAt(0);
        LinkedHashMap<String, String> resultList = new LinkedHashMap<>();

        for(Row row : sheet){
            if(row.getRowNum()==0){
                continue;
            }
            resultList.put(
                    row.getCell(0).getStringCellValue(),
                    row.getCell(1).getStringCellValue()
            );
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