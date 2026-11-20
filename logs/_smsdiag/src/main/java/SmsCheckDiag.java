import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeResponse;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.models.Config;

/**
 * 核验链路诊断：SendSmsVerifyCode(ReturnVerifyCode=true) 取回平台生成的验证码，
 * 再按 NumberAuthService.checkSmsVerifyCode 的同样参数调 CheckSmsVerifyCode，打印真实返回/异常。
 */
public class SmsCheckDiag {
    public static void main(String[] args) {
        String phone = args.length > 0 ? args[0] : "";
        try {
            Config config = new Config()
                .setAccessKeyId("")
                .setAccessKeySecret("")
                .setEndpoint("dypnsapi.aliyuncs.com");
            Client client = new Client(config);

            SendSmsVerifyCodeRequest sendReq = new SendSmsVerifyCodeRequest()
                .setPhoneNumber(phone)
                .setSignName("恒创联众")
                .setTemplateCode("100001")
                .setCountryCode("86")
                .setCodeLength(6L)
                .setValidTime(300L)
                .setDuplicatePolicy(1L)
                .setInterval(60L)
                .setTemplateParam("{\"code\":\"##code##\",\"min\":\"5\"}")
                .setCodeType(1L)
                .setReturnVerifyCode(true);
            SendSmsVerifyCodeResponse sendResp = client.sendSmsVerifyCode(sendReq);
            System.out.println("== send Code=" + sendResp.getBody().getCode()
                + " message=" + sendResp.getBody().getMessage());
            String code = sendResp.getBody().getModel() == null ? null : sendResp.getBody().getModel().getVerifyCode();
            System.out.println("== returned verifyCode=" + code);
            if (code == null) {
                return;
            }

            CheckSmsVerifyCodeRequest checkReq = new CheckSmsVerifyCodeRequest()
                .setPhoneNumber(phone)
                .setVerifyCode(code)
                .setCountryCode("86");
            CheckSmsVerifyCodeResponse checkResp = client.checkSmsVerifyCode(checkReq);
            System.out.println("== check Code=" + checkResp.getBody().getCode()
                + " message=" + checkResp.getBody().getMessage()
                + " verifyResult=" + (checkResp.getBody().getModel() == null ? null : checkResp.getBody().getModel().getVerifyResult()));
        } catch (TeaException e) {
            System.out.println("== TeaException code=" + e.getCode()
                + " statusCode=" + e.getStatusCode()
                + " message=" + e.getMessage()
                + " data=" + e.getData());
        } catch (Exception e) {
            System.out.println("== Exception " + e.getClass().getName() + ": " + e.getMessage());
            Throwable c = e.getCause();
            while (c != null) {
                System.out.println("   cause: " + c.getClass().getName() + ": " + c.getMessage());
                c = c.getCause();
            }
        }
    }
}
