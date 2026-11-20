import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.models.Config;

/**
 * 生产短信故障诊断：以与 NumberAuthService.sendSmsVerifyCode 完全相同的参数直连阿里云 PNVS，
 * 打印真实错误码/错误信息（TeaException 或 body Code），定位"验证码短信发送服务暂不可用"根因。
 */
public class SmsDiag {
    public static void main(String[] args) {
        String phone = args.length > 0 ? args[0] : "";
        try {
            Config config = new Config()
                .setAccessKeyId("")
                .setAccessKeySecret("")
                .setEndpoint("dypnsapi.aliyuncs.com");
            Client client = new Client(config);
            SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                .setPhoneNumber(phone)
                .setSignName("恒创联众")
                .setTemplateCode("100001")
                .setCountryCode("86")
                .setCodeLength(6L)
                .setValidTime(300L)
                .setDuplicatePolicy(1L)
                .setInterval(60L)
                .setTemplateParam("{\"code\":\"##code##\",\"min\":\"5\"}")
                .setCodeType(1L);
            SendSmsVerifyCodeResponse response = client.sendSmsVerifyCode(request);
            System.out.println("== HTTP 调用成功 body.Code=" + response.getBody().getCode()
                + " message=" + response.getBody().getMessage()
                + " requestId=" + response.getBody().getRequestId());
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
