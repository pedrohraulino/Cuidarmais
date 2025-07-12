package cuidar.mais.api.service;

import cuidar.mais.api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Set;

@Service
public class ImagemBase64Service {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    public String converterParaBase64(MultipartFile arquivo) throws IOException {
        // Validar arquivo
        validarArquivo(arquivo);

        // Converter para Base64
        byte[] bytes = arquivo.getBytes();
        return Base64.getEncoder().encodeToString(bytes);
    }

    public String processarBase64String(String base64String) {
        // Remove prefixo "data:image/...;base64," se existir
        if (base64String.startsWith("data:")) {
            int commaIndex = base64String.indexOf(",");
            if (commaIndex > 0) {
                return base64String.substring(commaIndex + 1);
            }
        }
        return base64String;
    }

    public String extrairTipoMime(String dataUrl) {
        if (dataUrl.startsWith("data:")) {
            int semicolonIndex = dataUrl.indexOf(";");
            if (semicolonIndex > 0) {
                return dataUrl.substring(5, semicolonIndex);
            }
        }
        return null;
    }

    private void validarArquivo(MultipartFile arquivo) throws IOException {
        if (arquivo.isEmpty()) {
            throw new IllegalArgumentException("Arquivo não pode estar vazio");
        }

        if (arquivo.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Arquivo muito grande (máximo 5MB)");
        }

        String contentType = arquivo.getContentType();
        if (!TIPOS_PERMITIDOS.contains(contentType)) {
            throw new IllegalArgumentException("Tipo de arquivo não permitido: " + contentType);
        }
    }

    public byte[] decodificarBase64(String base64String) {
        try {
            return Base64.getDecoder().decode(base64String);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("String Base64 inválida", e);
        }
    }

    // Método para redimensionar imagem (opcional)
    public String redimensionarImagem(String base64String, int larguraMax, int alturaMax) {
        try {
            byte[] imageBytes = decodificarBase64(base64String);
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));

            // Calcular novas dimensões mantendo proporção
            int larguraOriginal = originalImage.getWidth();
            int alturaOriginal = originalImage.getHeight();

            double ratioLargura = (double) larguraMax / larguraOriginal;
            double ratioAltura = (double) alturaMax / alturaOriginal;
            double ratio = Math.min(ratioLargura, ratioAltura);

            int novaLargura = (int) (larguraOriginal * ratio);
            int novaAltura = (int) (alturaOriginal * ratio);

            // Redimensionar
            BufferedImage resizedImage = new BufferedImage(novaLargura, novaAltura, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = resizedImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(originalImage, 0, 0, novaLargura, novaAltura, null);
            g2d.dispose();

            // Converter de volta para Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, "jpeg", baos);
            byte[] resizedBytes = baos.toByteArray();

            return Base64.getEncoder().encodeToString(resizedBytes);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao redimensionar imagem", e);
        }
    }
}