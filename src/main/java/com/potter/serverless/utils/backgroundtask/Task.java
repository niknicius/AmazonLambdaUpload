package com.potter.serverless.utils.backgroundtask;

import java.io.IOException;

public interface Task {

    Object run() throws InterruptedException, IOException;

}
