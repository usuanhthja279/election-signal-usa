package com.electionsignal;
import static org.junit.jupiter.api.Assertions.*;
import com.electionsignal.polling.PollingForecastClient;
import java.util.*;
import org.junit.jupiter.api.Test;
class PollingForecastClientTests {
    @Test void licenseAndShapeMustMatch(){
        var row=Map.of("key","G-TX","state","TX","licence","CC BY 4.0","chance_left",0.0);
        var doc=Map.<String,Object>of("dataset","governors","description","Fictional 2026 forecast","asOf","2026-10-01","licence","CC BY 4.0","count",1,"rows",List.of(row));
        assertDoesNotThrow(()->PollingForecastClient.validate("governors",doc));
        var rejected=new HashMap<>(doc);rejected.put("licence","UNKNOWN");assertThrows(IllegalArgumentException.class,()->PollingForecastClient.validate("governors",rejected));
        rejected.put("licence","CC BY 4.0");rejected.put("count",2);assertThrows(IllegalArgumentException.class,()->PollingForecastClient.validate("governors",rejected));
    }
}

