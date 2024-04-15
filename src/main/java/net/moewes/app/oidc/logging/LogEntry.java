package net.moewes.app.oidc.logging;

import com.microsoft.azure.storage.table.TableServiceEntity;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LogEntry extends TableServiceEntity {

    private String path = "";

    private String nonce = "";
    private String state = "";

    public LogEntry(String partitionKey, String rowKey) {
        super();
        setPartitionKey(partitionKey);
        setRowKey(rowKey);
    }
}
