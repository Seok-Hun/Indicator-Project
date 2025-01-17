import org.apache.poi.ss.usermodel.Workbook;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.IOException;

public class ShutDownHook extends Thread{
    private ChromeDriver driver;
    private Workbook workbook;
    public ShutDownHook(ChromeDriver driver, Workbook workbook){
        this.driver = driver;
        this.workbook = workbook;
    }
    @Override
    public void run(){
        if(driver!=null){
            System.out.println("driver 종료");
            driver.quit();
        }
        if(workbook!=null){
            try {
                System.out.println("workbook 종료");
                workbook.close();
            } catch (IOException e){
                System.out.println("workbook 종료 중 에러");
                e.printStackTrace();
            }
        }
    }
}
