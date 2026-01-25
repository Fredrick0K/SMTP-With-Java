package code;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;

public class EmailSender {

    // Gmail credentials - replace with yours
    private static final String USERNAME = "YOUR EMAIL HERE!!"; // Your Gmail account
    private static final String PASSWORD = "YOUR 16-DIGIT long HERE!!!"; // 16-Digit long code. Get from Google App Passwords

    public static void sendEmail(String toEmail, String subject, String body) {
        sendEmailWithAttachment(toEmail, subject, body, null);
    }

    public static void sendEmailWithAttachment(String toEmail, String subject, String body, String filePath) {

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, PASSWORD);
            }
        });

        try {
            //Objects declaration
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(USERNAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);

            // Create message body part
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(body);

            // Create multipart message
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);

            // Add attachment if provided
            if (filePath != null && !filePath.isEmpty()) {
                MimeBodyPart attachmentPart = new MimeBodyPart();
                DataSource source = new FileDataSource(filePath);
                attachmentPart.setDataHandler(new DataHandler(source));
                attachmentPart.setFileName(new java.io.File(filePath).getName());
                multipart.addBodyPart(attachmentPart);
            }

            message.setContent(multipart);

            System.out.println("Sending email...");
            Transport.send(message);
            System.out.println("Email sent successfully to " + toEmail);

        } catch (MessagingException e) {
            System.err.println("Failed to send email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Send email WITHOUT attachment
        // sendEmail(
        // "jocace1506@imfaya.com",
        // "Test Email",
        // "Yo! This is a test email from Java."
        // );

        // Send email WITH attachment
        sendEmailWithAttachment(
                "RECIEVER EMAIL!", //Reciever
                "Email with File", //Asunto
                "Check out this attachment!", //Text
                "smtp\\attachment\\ello.gif"); //Attachment URL, this URL here containns a gif.
    }
}
