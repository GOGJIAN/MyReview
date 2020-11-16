public class ProducerConsumer {
    public static void main(String args[]) {
        Buffer publicBuffer = new Buffer();
        Produer p1 = new Produer(publicBuffer);
        Produer p2 = new Produer(publicBuffer);
        Produer p3 = new Produer(publicBuffer);
        Consumer c1 = new Consumer(publicBuffer);
        Consumer c2 = new Consumer(publicBuffer);

        p1.start();
        p2.start();
        p3.start();
        c1.start();
        c2.start();
    }
}

class Buffer {
    private int size = 10;
    private int num = 0;

    synchronized void consume() {
        try {
            if (num <= 0) {
                wait();
            } else {
                num--;
                System.out.println("消费者" + Thread.currentThread().getName() + "消费了1个," + "当前还有" + num + "个");
                notifyAll();

            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    synchronized void product() {
        try {
            if (num >= size) {
                wait();
            } else {
                num++;
                System.out.println(String.format("生产者%s生产了1个,当前还有%d个", Thread.currentThread().getName(), num));
                notifyAll();

            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class Consumer extends Thread {
    private Buffer mBuffer;

    Consumer(Buffer buffer) {
        mBuffer = buffer;
    }

    @Override
    public void run() {
        while (true) {
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mBuffer.consume();
        }
    }
}

class Produer extends Thread {
    private Buffer mBuffer;

    Produer(Buffer buffer) {
        mBuffer = buffer;
    }

    @Override
    public void run() {
        while(true) {
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mBuffer.product();
        }
    }
}
