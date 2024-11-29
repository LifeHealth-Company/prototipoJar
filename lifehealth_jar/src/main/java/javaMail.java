import javax.mail.*;
import javax.mail.internet.*;
import java.io.*;
import java.util.*;
import javax.activation.*;

public class javaMail {

    // Método para enviar o e-mail com anexo
    public static void enviarEmailComAnexo(String destinatario, String assunto, String mensagem, String logFilePath) throws MessagingException, IOException {

        final String d_email = ("mauricio.almeida@sptech.school");
        final String d_password = ("Maumau2000@");

        Properties props = new Properties();
        props.put("mail.smtp.user", d_email);
        props.put("mail.smtp.host", "smtp-mail.outlook.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.auth", "true");

        try {
            Authenticator auth = new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(d_email, d_password);
                }
            };

            // Criação da sessão com as propriedades e autenticação
            Session session = Session.getInstance(props, auth);

            // Criando a mensagem
            MimeMessage msg = new MimeMessage(session);
            msg.setSubject(assunto);  // Usando o assunto do parâmetro
            msg.setFrom(new InternetAddress(d_email));  // E-mail de envio
            msg.addRecipient(Message.RecipientType.TO, new InternetAddress(destinatario));  // E-mail de destino

            // Criando o corpo do e-mail
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(mensagem); // Corpo do e-mail vindo do parâmetro

            // Criando o anexo (arquivo de log)
            MimeBodyPart attachmentPart = new MimeBodyPart();
            FileDataSource source = new FileDataSource(logFilePath);  // Caminho do arquivo de log
            attachmentPart.setDataHandler(new DataHandler(source));
            attachmentPart.setFileName("logfile.log");

            // Combinando as partes (corpo do e-mail e anexo)
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(textPart);
            multipart.addBodyPart(attachmentPart);

            // Definindo o conteúdo multipart no e-mail
            msg.setContent(multipart);

            // Enviar o e-mail
            Transport.send(msg);

            System.out.println("Email com o log enviado com sucesso!");

        } catch (MessagingException mex) {
            mex.printStackTrace();
        }
    }
}
