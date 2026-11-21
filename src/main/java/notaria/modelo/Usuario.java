package notaria.modelo;

import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Funcionario que entra al sistema (vista lógica).
 * La contraseña NUNCA se guarda tal cual: se guarda cifrada (PBKDF2),
 * por eso el atributo es privado y solo se cambia con cambiarContrasena().
 */
public class Usuario {

    private String nombreUsuario;
    private String correo;
    private String contrasena;   // contraseña cifrada, no la original
    private boolean activo;
    private Rol rol;             // Usuario * ---- 1 Rol

    public Usuario(String nombreUsuario, String correo, Rol rol) {
        if (nombreUsuario == null || nombreUsuario.isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio");
        }
        this.nombreUsuario = nombreUsuario.trim();
        this.correo = correo;
        this.rol = rol;
        this.activo = true;
    }

    /** Recibe la contraseña escrita por la persona y guarda su versión cifrada. */
    public void cambiarContrasena(String contrasenaPlana) {
        if (contrasenaPlana == null || contrasenaPlana.length() < 4) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 4 caracteres");
        }
        byte[] sal = new byte[16];
        new SecureRandom().nextBytes(sal);
        this.contrasena = Base64.getEncoder().encodeToString(sal) + ":"
                + Base64.getEncoder().encodeToString(cifrar(contrasenaPlana, sal));
    }

    /** Compara la contraseña escrita con la cifrada que está guardada. */
    public boolean verificarContrasena(String contrasenaPlana) {
        if (contrasena == null || contrasenaPlana == null) {
            return false;
        }
        String[] partes = contrasena.split(":");
        byte[] sal = Base64.getDecoder().decode(partes[0]);
        String calculada = Base64.getEncoder().encodeToString(cifrar(contrasenaPlana, sal));
        return calculada.equals(partes[1]);
    }

    private static byte[] cifrar(String texto, byte[] sal) {
        try {
            KeySpec spec = new PBEKeySpec(texto.toCharArray(), sal, 65536, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo cifrar la contraseña", e);
        }
    }

    // ----- getters y setters -----

    public String getNombreUsuario() { return nombreUsuario; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }

    /** Solo para guardar/leer de la base de datos (ya viene cifrada). */
    public String getContrasenaCifrada() { return contrasena; }
    public void setContrasenaCifrada(String cifrada) { this.contrasena = cifrada; }
}
