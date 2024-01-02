package fr.vcity.sparqltosql.dao;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Table("version")
public class Version {
    @Id
    @Schema(name = "Index version", example = "1")
    private Integer indexVersion;

    @Schema(name = "Message of the new version", example = "Inserted trees and removed building")
    private String message;

    @Schema(name = "Start of the transaction date", example = "2023-07-17 11:20:00+02")
    private LocalDateTime transactionTimeStart;

    @Schema(name = "End of the transaction date", example = "2023-07-18 20:00:00+02")
    private LocalDateTime transactionTimeEnd;

    public Version() {
    }

    public Version(
            Integer indexVersion,
            String message,
            LocalDateTime transactionTimeStart,
            LocalDateTime transactionTimeEnd
    ) {
        this.indexVersion = indexVersion;
        this.message = message;
        this.transactionTimeStart = transactionTimeStart;
        this.transactionTimeEnd = transactionTimeEnd;
    }
}
