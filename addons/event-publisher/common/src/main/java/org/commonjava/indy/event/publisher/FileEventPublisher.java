package org.commonjava.indy.event.publisher;

import org.commonjava.event.file.FileEvent;

public interface FileEventPublisher
{
    void publishFileEvent( FileEvent fileEvent );
}
