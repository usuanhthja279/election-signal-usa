package com.electionsignal.sources;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class NorthCarolinaArchiveCommandTests {
    @Test void checksApprovalBeforeAnyDownload() {
        var archive=mock(RawSourceArchive.class);
        doThrow(new SecurityException("not approved")).when(archive).requireAuthorized("NC_2026_OFFICIAL", NorthCarolinaResultsPreflight.URL.toString());
        var command=new NorthCarolinaArchiveCommand(archive);
        assertThrows(SecurityException.class,()->command.run(new DefaultApplicationArguments(new String[0])));
        verify(archive).requireAuthorized("NC_2026_OFFICIAL", NorthCarolinaResultsPreflight.URL.toString());
        verifyNoMoreInteractions(archive);
    }
}
