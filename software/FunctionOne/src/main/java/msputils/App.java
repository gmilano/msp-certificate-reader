package msputils;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.util.HashMap;
import java.util.Map;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.util.Base64;
import com.upokecenter.cbor.CBORObject;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.json.JSONObject;
import se.digg.dgc.encoding.impl.DefaultBarcodeDecoder;
import se.digg.dgc.signatures.cose.CoseSign1_Object;
import se.digg.dgc.signatures.cwt.Cwt;
import java.security.PublicKey;
import java.io.ByteArrayOutputStream;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import javax.imageio.ImageIO;
// Bar Code
import se.digg.dgc.encoding.Barcode;
import se.digg.dgc.encoding.BarcodeException;
import se.digg.dgc.encoding.Base45;
import se.digg.dgc.encoding.DGCConstants;
import se.digg.dgc.encoding.Zlib;
// PDF Handling
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.tracing.Tracing;
import static software.amazon.lambda.powertools.tracing.CaptureMode.DISABLED;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Tracing(captureMode = DISABLED)
    @Metrics(captureColdStart = true)
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {

        LambdaLogger logger = context.getLogger();
        logger.log("Starting request");
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");

   
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withHeaders(headers);
        try {
             String pageContents;
            if (input != null && input.getIsBase64Encoded())
            {
                logger.log("env:" + System.getenv("pubkey"));
                
                String pdfContents64 = input.getBody();
                pageContents = validatePDFVaccineCertificate(System.getenv("pubkey"), pdfContents64, logger);
                return response
                        .withStatusCode(200)
                        .withBody(pageContents.substring(1, pageContents.length() - 1).replace("\\", ""));
            }
            else
            {
                return response
                .withStatusCode(200)
                .withBody(String.format("{ \"code\": \"400\", \"info\": \"%s\" }", "Invalid format"));
            }
        } catch (SignatureException | CertificateException e) {
            return response
            .withBody(e.getMessage())
            .withStatusCode(500);
        } 
    }

 
   
    public static PublicKey generatePublicKey(String pubkey, LambdaLogger logger){
        try{
            String pubKeyPEM = pubkey;
            byte[] byteKey = Base64.decode(pubKeyPEM);
            java.security.spec.X509EncodedKeySpec pubkeyVal = new java.security.spec.X509EncodedKeySpec(byteKey);
            var kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(pubkeyVal);
        }
        catch(Exception e){
            logger.log("ERROR: generating public key");
            return null;
        }
    }

  
    public static String validateSigned(String publicKey, String data, LambdaLogger logger) throws IOException, SignatureException, CertificateException {
        var response = new JSONObject();
        try {
            byte[] image = Base64.decode(data);
            DefaultBarcodeDecoder defbarcodeDecoder = new DefaultBarcodeDecoder();
            var base45 = defbarcodeDecoder.decodeToString(image, Barcode.BarcodeType.QR, null);
            if (base45.startsWith(DGCConstants.DGC_V1_HEADER)) {
               base45 = base45.substring(DGCConstants.DGC_V1_HEADER.length());
            }
            // Base45 decode into a compressed CWT ...
            //
            byte[] compressedCwt = Base45.getDecoder().decode(base45);
            // De-compress
            if (!Zlib.isCompressed(compressedCwt)) {
            }
            byte[] cwt = Zlib.decompress(compressedCwt, false);
            // OK, we now have the uncompressed CWT, lets verify it ...
            CoseSign1_Object cose = CoseSign1_Object.decode(cwt);
            cose.verifySignature(generatePublicKey(publicKey, logger));
            var cwtVerif = cose.getCwt();
            CBORObject cbor = cwtVerif.getClaim(99);
            return cbor.ToJSONString();
       } catch ( SignatureException  | BarcodeException e) {
           buildResponse(response, false, e.getMessage());
       } 
        return response.toString();
    }

    private static void buildResponse(JSONObject response, boolean ok, Object out) {
        response.put("Success", ok);
        response.put("MessageError", "");
        response.put("Payload",out);
    }
 
    public static String validatePDFVaccineCertificate(String pubKey,String pdfContents, LambdaLogger logger) 
        throws SignatureException, CertificateException{
        var payload  = "";
        var response = new JSONObject();
        String qrimageB64 = extraeQRImageB64(pdfContents, logger);
            
        if (!qrimageB64.equals("")) {
            try {
                payload = validateSigned(pubKey, qrimageB64, logger);
            } catch (IOException e) {
              logger.log(String.format("ERROR: validating certificate , %s", e.getMessage()));
            }
        } else {
            response.put("Success", "false");
            response.put("MessageError", "No se pudo extraer el QRCode del PDF ingresado.");
            response.put("Payload","");
            payload = response.toString();
        }
        return payload;
    }

    @Tracing(namespace = "extraeQRImageB64")
    public static String extraeQRImageB64(String base64Contents, LambdaLogger logger) {
        var qrImage = "";
        try (PDDocument document = PDDocument.load(Base64.decode(base64Contents))) {
            PDPageTree pages = document.getPages();
            for (PDPage pdfpage : pages) {
                PDResources pdResources = pdfpage.getResources();
                for (COSName cname : pdResources.getXObjectNames()) {
                    PDXObject object = pdResources.getXObject(cname);
                    if (object instanceof PDImageXObject) {
                        ByteArrayOutputStream qrImageStream = new ByteArrayOutputStream();
                        ImageIO.write(((PDImageXObject) object).getImage(), "png", qrImageStream);
                       
                        byte[] bytesQR = Base64.encode(qrImageStream.toByteArray()); 
                        qrImage = new String(bytesQR);
                        return qrImage;
                    }
                }
            }
        } catch (Exception ex) {
            logger.log(String.format("ERROR: analyzing pdf , %s", ex.getMessage()));
        }
        return qrImage;
    }
}