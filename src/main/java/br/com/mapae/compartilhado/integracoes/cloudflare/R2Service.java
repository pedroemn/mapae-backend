package br.com.mapae.compartilhado.integracoes.cloudflare;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
public class R2Service implements ArmazenamentoService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;
    private final String publicUrl;

    public R2Service(
            S3Client s3Client,
            S3Presigner s3Presigner,
            @Value("${CLOUDFLARE_R2_BUCKET}") String bucketName,
            @Value("${CLOUDFLARE_R2_PUBLIC_URL:}") String publicUrl) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
        this.publicUrl = publicUrl;
    }

    /**
     * Retorna a URL pública direta da CDN (ex: https://midias.mapae.com.br/locais/123.jpg).
     * O app baixa direto da CDN da Cloudflare com cache em borda.
     */
    @Override
    public String obterUrlPublica(String chaveObjeto) {
        if (chaveObjeto == null || chaveObjeto.isBlank()) {
            return null;
        }
        if (publicUrl != null && !publicUrl.isBlank()) {
            return publicUrl.endsWith("/") ? publicUrl + chaveObjeto : publicUrl + "/" + chaveObjeto;
        }
        return chaveObjeto;
    }

    /**
     * Gera URL temporária pré-assinada para exibição de arquivos restritos.
     */
    @Override
    public String gerarUrlPreAssinadaParaVisualizacao(String chaveObjeto, Duration duracao) {
        if (chaveObjeto == null || chaveObjeto.isBlank()) {
            return null;
        }

        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(chaveObjeto)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(duracao)
                .getObjectRequest(getRequest)
                .build();

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
        return presigned.url().toString();
    }

    /**
     * Gera uma URL de PUT para o app enviar qualquer foto (pública ou privada) diretamente ao R2.
     */
    @Override
    public String gerarUrlPreAssinadaParaUpload(String chaveObjeto, String contentType, Duration duracao) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(chaveObjeto)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(duracao)
                .putObjectRequest(putRequest)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
        return presigned.url().toString();
    }

    @Override
    public void excluirArquivo(String chaveObjeto) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(chaveObjeto)
                .build();

        s3Client.deleteObject(deleteRequest);
    }
}