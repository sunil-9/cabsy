package firebase.cabsy.Common;

import firebase.cabsy.Remote.FCMClient;
import firebase.cabsy.Remote.IFCMService;

public class Common {

    public static String fcmURL = "https://fcm.googleapis.com/";
    public static String className;
    public static String userID = null;

    public static IFCMService getFCMService(){
        return FCMClient.getClient(fcmURL).create(IFCMService.class);
    }

    public static String getClassName() {
        return className;
    }

    public static void setClassName(String className) {
        Common.className = className;
    }
}
