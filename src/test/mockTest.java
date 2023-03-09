import org.junit.Test;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class mockTest {
    @Test
    public void  mockVerify(){
        List<String> mock =mock(List.class);
        mock.add("one");

        mock.clear();

        verify(mock).add("one");
        verify(mock).clear();

    }






}
