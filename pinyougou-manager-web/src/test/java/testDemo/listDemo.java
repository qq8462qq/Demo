package testDemo;

import com.pinyougou.manager.controller.LoginController;

import java.util.Map;

public class listDemo {
    public static void main(String[] args) {
        LoginController loginController = new LoginController();
        Map name = loginController.name();
        System.out.println(name);


    }

}
