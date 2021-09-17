package net.moewes.app.oidc;

public class FormLayout {

    static private final String FORM_PAGE_STYLE =
            "display: flex;" +
                    "justify-content: center;";

    static private final String FORM_CONTAINER_STYLE =
            "width: 1000px";

    static private final String FORMITEM_CONTAINER_STYLE = "" +
            "display: flex; " +
            "flex-wrap: wrap; " +
            "align-items: center; " +
            "overflow: hidden;" +
            "margin: 0.5rem;";
    static private final String LABEL_CONTAINER_STYLE = "" +
            "padding-right: 2rem;" +
            "min-width: 25%;" +
            "width: calc((25em - 100%) * 1000);" +
            "max-width: 50%;";
    static private final String FIELD_CONTAINER_STYLE = "" +
            "padding-right: 1rem;" +
            "padding-left: 0rem;" +
            "width: 65%;" +
            "min-width: 290px;" +
            "max-width: 100%;" +
            "";

    public static String getLoginForm() {

        StringBuilder sb = new StringBuilder();

        sb.append("<!doctype html>");
        sb.append("<html>");
        sb.append("<head>");
        sb.append("<meta charset=\"utf-8\">");
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        sb.append("<script src=\"/webjars/cloud-ui-oidc-ui5/0.1.0/index.js\"></script>");
        sb.append("</head>");
        sb.append("<body>");
        sb.append("<div style=\"height: 100vh;\">");
        sb.append("<ui5-page background-design=\"Solid\">");
        sb.append("<form method=\"post\">");
        sb.append("<div style=\"" + FORM_PAGE_STYLE + "\">");
        sb.append("<div style=\"" + FORM_CONTAINER_STYLE + "\">");
        sb.append("<div style=\"" + FORMITEM_CONTAINER_STYLE + "\">");
        sb.append("<div style=\"" + LABEL_CONTAINER_STYLE + "\">");
        sb.append("<ui5-label for=\"fname\">Username</ui5-label>");
        sb.append("</div>");
        sb.append("<div style=\"" + FIELD_CONTAINER_STYLE + "\">");
        sb.append("<ui5-input type=\"text\" id=\"username\" name=\"username\"></ui5-input><br><br>");
        sb.append("</div>");
        sb.append("</div>");
        sb.append("<div style=\"" + FORMITEM_CONTAINER_STYLE + "\">");
        sb.append("<div style=\"" + LABEL_CONTAINER_STYLE + "\">");
        sb.append("<ui5-label for=\"lname\">Password</ui5-label>");
        sb.append("</div>");
        sb.append("<div style=\"" + FIELD_CONTAINER_STYLE + "\">");
        sb.append("<ui5-input type=\"text\" id=\"lname\" name=\"lname\"></ui5-input><br><br>");
        sb.append("</div>");
        sb.append("</div>");
        sb.append("<ui5-button submits=\"true\" >Login</ui5-button>");
        sb.append("</div>");
        sb.append("</div>");
        sb.append("</form>");
        sb.append("</ui5-page>");
        sb.append("</div>");
        sb.append("</body>");
        sb.append("</html>");

        return sb.toString();
    }
}
