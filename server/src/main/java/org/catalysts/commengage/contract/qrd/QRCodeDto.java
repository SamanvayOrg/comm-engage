package org.catalysts.commengage.contract.qrd;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.catalysts.commengage.domain.QRCode;

import java.time.LocalDateTime;

@Getter
@Setter
@EqualsAndHashCode()
@ToString
public class QRCodeDto {
    @JsonProperty("id")
    private String qrdid;

    private String shorturl;

    private String folder;

    private int scans;

    private int uniquevisitors;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime creationdate;

    @JsonIgnore
    public QRCode toEntity() {
        QRCode qrCode = new QRCode();
        qrCode.setQrdId(this.qrdid);
        qrCode.setShortUrl(this.shorturl);
        qrCode.setUniqueVisitors(this.uniquevisitors);
        qrCode.setScans(this.scans);
        qrCode.setFolder(this.folder);
        qrCode.setCreationDate(this.creationdate);
        return qrCode;
    }

    @JsonIgnore
    public void updateEntity(QRCode qrCode) {
        qrCode.setShortUrl(this.shorturl);
        qrCode.setUniqueVisitors(this.uniquevisitors);
        qrCode.setScans(this.scans);
        qrCode.setFolder(this.folder);
    }
}