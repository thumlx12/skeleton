package controllers;

import api.CreateReceiptRequest;
import dao.ReceiptDao;
import dao.TagDao;
import org.junit.Test;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class TagControllerTest {
    @Test
    public void testTagController() {
        TagDao tags = mock(TagDao.class);
        TagController tagController = new TagController(tags);

        tagController.toggleTag("fat", 2);
        tagController.getTaggedReceipts("fat");
        tagController.toggleTag("fat", 2);

        verify(tags).getAllTaggedReceipts("fat");
        verify(tags,times(2)).insert("fat",2);

        verifyNoMoreInteractions(tags);
    }
}
