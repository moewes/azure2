package net.moewes.app.oidc;

public class FormLayout {

    public static String getLoginForm( String message) {

        StringBuilder sb = new StringBuilder();

        sb.append("<!doctype html>");
        sb.append("<html>");
        sb.append("<head>");
        sb.append("<meta charset=\"utf-8\">");
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        sb.append("<script src=\"/webjars/cloud-ui-ui5/0.3.0/index.js\"></script>");
        sb.append(getStyles());

        sb.append("</head>");
        sb.append("<body style=\"box-sizing: border-box;\">");

        sb.append("<ui5-dialog id=\"login-dialog\" header-text=\"Login\" open>" +
                "<section " +
                "class=\"login-form\">");

        sb.append("<div>");
        sb.append("<ui5-label for=\"username\" required>Username: </ui5-label>");
        sb.append("<ui5-input id=\"ui_username\"></ui5-input>");
        sb.append("</div>");
        sb.append("<div>");
        sb.append("<ui5-label for=\"password\" required>Password: </ui5-label>");
        sb.append("<ui5-input id=\"ui_password\" type=\"Password\" ></ui5-input>");
        sb.append("</div>");
        sb.append("</section>");
        sb.append("<div slot=\"footer\" class=\"dialog-footer\" >");
        sb.append("<ui5-button id=\"login_button\" design=\"Emphasized\" " +
                " >Login</ui5-button>");
        sb.append("</div>");
        if (message != null) {
            sb.append("<div>");
            sb.append("<ui5-message-strip hide-close-button design=\"Warning\">" + message
                    + "</ui5-message-strip>");
            sb.append("</div>");
        }
        sb.append("</ui5-dialog>");

        sb.append("<form method=\"post\" id=\"login_form\">");
        sb.append(
                "<input type=\"text\" id=\"username\" name=\"username\">" +
                        "<input type=\"Password\" id=\"password\" name=\"password\" >");
        sb.append("</form>");
        sb.append("<script>" +
                "var loginbutton = document.getElementById(\"login_button\");" +
                "var ui_username = document.getElementById('ui_username');" +
                "var form_username = document.getElementById('username');" +
                "var ui_password = document.getElementById('ui_password');" +
                "var form_password = document.getElementById('password');" +
                "var form= document.getElementById('login_form');" +
                "loginbutton.addEventListener(\"click\", function() {" +
                "document.getElementById('username').value = ui_username.value;" +
                "form_password.value = ui_password.value;" +
                "form.submit();" +
                "});" +
                "</script>");
        sb.append("</body>");
        sb.append("</html>");

        return sb.toString();
    }

    private static String getStyles() {
        return "<style>" +
                ".login-form {" +
                "display: flex;" +
                "flex-direction: column;" +
                "justify-content: space-evenly;" +
                "align-items: flex-start;" +
                "margin: 3rem 6rem;" +
                "}" +
                ".login-form > div {" +
                "display: grid;" +
                "width: 15rem;" +
                "margin-bottom: .5rem;}" +
                ".dialog-footer {" +
                "display: flex;" +
                "align-items: center;" +
                "justify-content: flex-end;" +
                "width: 100%;" +
                "padding: .5rem 0rem;" +
                "}" +
                "@media(max-width: 600px) {" +
                ".login-form {" +
                "margin: 1rem 2rem;" +
                "}" +
                "</style>";
    }
}
