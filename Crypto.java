import javax.crypto.*;
import javax.crypto.spec.*;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import java.util.*;

/**
 * Cifrado híbrido: RSA para la clave + AES para los datos
 * Permite cifrar archivos de cualquier tamaño
 */
public class CifradoHibrido {
    
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    
    /**
     * Cifra un archivo usando cifrado híbrido (RSA + AES)
     */
    public static File cifrarArchivoGrande(String archivoOriginal, String clavePublicaReceptor) 
            throws Exception {
        
        System.out.println("🔐 Iniciando cifrado híbrido...");
        
        // 1. Generar clave AES aleatoria
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey claveAES = keyGen.generateKey();
        System.out.println("✓ Clave AES generada");
        
        // 2. Cifrar el archivo con AES
        byte[] contenidoOriginal = Files.readAllBytes(Paths.get(archivoOriginal));
        Cipher cipherAES = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipherAES.init(Cipher.ENCRYPT_MODE, claveAES);
        byte[] iv = cipherAES.getIV(); // Vector de inicialización
        byte[] archivoCifradoAES = cipherAES.doFinal(contenidoOriginal);
        System.out.println("✓ Archivo cifrado con AES (" + archivoCifradoAES.length + " bytes)");
        
        // 3. Cargar clave pública RSA del receptor
        byte[] bytesClavePublica = Files.readAllBytes(Paths.get(clavePublicaReceptor));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(bytesClavePublica);
        PublicKey clavePublica = keyFactory.generatePublic(keySpec);
        
        // 4. Cifrar la clave AES con RSA
        Cipher cipherRSA = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipherRSA.init(Cipher.ENCRYPT_MODE, clavePublica);
        byte[] claveAESCifrada = cipherRSA.doFinal(claveAES.getEncoded());
        System.out.println("✓ Clave AES cifrada con RSA");
        
        // 5. Combinar todo en un archivo
        String nombreSalida = archivoOriginal + ".hybrid.encrypted";
        try (FileOutputStream fos = new FileOutputStream(nombreSalida)) {
            // Escribir longitud de la clave cifrada (4 bytes)
            fos.write(intToBytes(claveAESCifrada.length));
            // Escribir clave AES cifrada
            fos.write(claveAESCifrada);
            // Escribir IV (16 bytes para AES)
            fos.write(iv);
            // Escribir datos cifrados
            fos.write(archivoCifradoAES);
        }
        
        System.out.println("✓ Archivo cifrado guardado: " + nombreSalida);
        return new File(nombreSalida);
    }
    
    /**
     * Descifra un archivo híbrido usando la clave privada
     */
    public static File descifrarArchivoGrande(String archivoCifrado, String miClavePrivada) 
            throws Exception {
        
        System.out.println("🔓 Iniciando descifrado híbrido...");
        
        // 1. Cargar clave privada RSA
        byte[] bytesClavePrivada = Files.readAllBytes(Paths.get(miClavePrivada));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(bytesClavePrivada);
        PrivateKey clavePrivada = keyFactory.generatePrivate(keySpec);
        
        // 2. Leer el archivo cifrado
        byte[] todoElArchivo = Files.readAllBytes(Paths.get(archivoCifrado));
        
        try (ByteArrayInputStream bis = new ByteArrayInputStream(todoElArchivo)) {
            // Leer longitud de clave cifrada
            byte[] longitudBytes = new byte[4];
            bis.read(longitudBytes);
            int longitudClaveCifrada = bytesToInt(longitudBytes);
            
            // Leer clave AES cifrada
            byte[] claveAESCifrada = new byte[longitudClaveCifrada];
            bis.read(claveAESCifrada);
            
            // Leer IV
            byte[] iv = new byte[16];
            bis.read(iv);
            
            // Leer datos cifrados
            byte[] datosCifrados = bis.readAllBytes();
            
            // 3. Descifrar la clave AES con RSA
            Cipher cipherRSA = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipherRSA.init(Cipher.DECRYPT_MODE, clavePrivada);
            byte[] claveAESBytes = cipherRSA.doFinal(claveAESCifrada);
            SecretKey claveAES = new SecretKeySpec(claveAESBytes, "AES");
            System.out.println("✓ Clave AES descifrada");
            
            // 4. Descifrar los datos con AES
            Cipher cipherAES = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipherAES.init(Cipher.DECRYPT_MODE, claveAES, ivSpec);
            byte[] datosOriginales = cipherAES.doFinal(datosCifrados);
            System.out.println("✓ Archivo descifrado");
            
            // 5. Guardar archivo descifrado con extensión original
            String nombreSalida;
            if (archivoCifrado.endsWith(".hybrid.encrypted")) {
                // Recuperar nombre original (sin .hybrid.encrypted)
                nombreSalida = archivoCifrado.replace(".hybrid.encrypted", "");
            } else {
                // Si no tiene el formato esperado, agregar .decrypted
                nombreSalida = archivoCifrado + ".decrypted";
            }
            
            Files.write(Paths.get(nombreSalida), datosOriginales);
            
            System.out.println("✓ Archivo guardado: " + nombreSalida);
            System.out.println("✓ Archivo listo para abrir directamente");
            return new File(nombreSalida);
        }
    }
    
    /**
     * Envía archivo cifrado híbrido por Gmail
     */
    public static void enviarArchivoHibrido(String emailRemitente, String passwordApp,
                                           String emailReceptor, String archivoOriginal,
                                           String clavePublicaReceptor) throws Exception {
        
        System.out.println("\n=== PROCESO DE CIFRADO Y ENVÍO HÍBRIDO ===\n");
        
        // 1. Cifrar archivo
        File archivoCifrado = cifrarArchivoGrande(archivoOriginal, clavePublicaReceptor);
        
        // 2. Configurar correo
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.ssl.trust", SMTP_HOST);
        
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(emailRemitente, passwordApp);
            }
        });
        
        // 3. Crear y enviar mensaje
        Message mensaje = new MimeMessage(session);
        mensaje.setFrom(new InternetAddress(emailRemitente));
        mensaje.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailReceptor));
        mensaje.setSubject("🔒 Archivo Cifrado (Híbrido RSA+AES)");
        
        BodyPart cuerpoMensaje = new MimeBodyPart();
        cuerpoMensaje.setText(
            "Hola,\n\n" +
            "Te envío un archivo cifrado usando cifrado híbrido RSA+AES.\n" +
            "Esto permite enviar archivos de cualquier tamaño de forma segura.\n\n" +
            "Archivo original: " + new File(archivoOriginal).getName() + "\n" +
            "Tamaño original: " + Files.size(Paths.get(archivoOriginal)) + " bytes\n" +
            "Archivo cifrado: " + archivoCifrado.getName() + "\n\n" +
            "Para descifrar:\n" +
            "1. Descarga el archivo adjunto\n" +
            "2. Usa: java CifradoHibrido descifrar [archivo] [tu_clave_privada]\n\n" +
            "Saludos seguros! 🔐"
        );
        
        MimeBodyPart adjunto = new MimeBodyPart();
        adjunto.attachFile(archivoCifrado);
        
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(cuerpoMensaje);
        multipart.addBodyPart(adjunto);
        
        mensaje.setContent(multipart);
        Transport.send(mensaje);
        
        System.out.println("\n✓ Correo enviado exitosamente");
        System.out.println("✅ PROCESO COMPLETADO\n");
    }
    
    // Métodos auxiliares para convertir int a bytes y viceversa
    private static byte[] intToBytes(int value) {
        return new byte[] {
            (byte)(value >>> 24),
            (byte)(value >>> 16),
            (byte)(value >>> 8),
            (byte)value
        };
    }
    
    private static int bytesToInt(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24) |
               ((bytes[1] & 0xFF) << 16) |
               ((bytes[2] & 0xFF) << 8) |
               (bytes[3] & 0xFF);
    }
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        try {
            System.out.println("╔════════════════════════════════════════════════╗");
            System.out.println("║  CIFRADO HÍBRIDO - Archivos de cualquier tamaño ║");
            System.out.println("╚════════════════════════════════════════════════╝\n");
            
            System.out.println("Opciones:");
            System.out.println("1. Cifrar y enviar archivo");
            System.out.println("2. Solo cifrar archivo (sin enviar)");
            System.out.println("3. Descifrar archivo recibido");
            System.out.print("\nSelecciona opción: ");
            
            int opcion = scanner.nextInt();
            scanner.nextLine();
            
            switch (opcion) {
                case 1:
                    System.out.print("\nTu email: ");
                    String email = scanner.nextLine();
                    System.out.print("Contraseña de aplicación: ");
                    String password = scanner.nextLine();
                    System.out.print("Email del receptor: ");
                    String emailReceptor = scanner.nextLine();
                    System.out.print("Archivo a enviar: ");
                    String archivo = scanner.nextLine();
                    System.out.print("Clave pública del receptor: ");
                    String clavePublica = scanner.nextLine();
                    
                    enviarArchivoHibrido(email, password, emailReceptor, archivo, clavePublica);
                    break;
                    
                case 2:
                    System.out.print("\nArchivo a cifrar: ");
                    String archivoACifrar = scanner.nextLine();
                    System.out.print("Clave pública del receptor: ");
                    String clavePublicaReceptor = scanner.nextLine();
                    
                    cifrarArchivoGrande(archivoACifrar, clavePublicaReceptor);
                    System.out.println("\n✅ Archivo cifrado correctamente");
                    break;
                    
                case 3:
                    System.out.print("\nArchivo cifrado: ");
                    String archivoCifrado = scanner.nextLine();
                    System.out.print("Tu clave privada: ");
                    String clavePrivada = scanner.nextLine();
                    
                    descifrarArchivoGrande(archivoCifrado, clavePrivada);
                    System.out.println("\n✅ Archivo descifrado correctamente");
                    break;
                    
                default:
                    System.out.println("Opción no válida");
            }
            
        } catch (Exception e) {
            System.err.println("\n❌ ERROR: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
}