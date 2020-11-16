
![sortO](/Assets/复杂度.png)

```java
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class Sort {

    public static void main(String args[]) {
        Sort sort = new Sort();
        int[] testData = new int[10000];
        Random random = new Random();
        for (int i = 0; i < testData.length; i++) {
            testData[i] = random.nextInt(100000);
        }
        //System.out.println(Arrays.toString(testData));
        //sort.quickSort(testData,0,testData.length-1);
        System.out.println(Arrays.toString(sort.bubbleSort(testData)));
    }

    private void quickSort(int[] nums, int start,int end){
        if (start>=end)
            return;
        int i = start;
        int j = end;
        int temp = nums[i];
        while(i<j){
            while(i<j&&nums[j]>=temp){
                j--;
            }
            if (i<j){
                nums[i] = nums[j];
                i++;
            }
            while (i<j&&nums[i]<=temp){
                i++;
            }
            if (i<j){
                nums[j] = nums[i];
                j--;
            }
        }
        nums[i] = temp;
        quickSort(nums,start,i-1);
        quickSort(nums,i+1,end);
    }

    private int[] radixSort(int[] nums){
        int times;
        int maxValue = nums[0];
        int minValue = nums[0];
        int mod = 10;
        int dev = 1;
        for (int i = 1; i < nums.length; i++) {
            if(maxValue<nums[i]){
                maxValue = nums[i];
            }
            if (minValue>nums[i]){
                minValue = nums[i];
            }
        }
        times = getDigit(maxValue)>getDigit(minValue)?getDigit(maxValue):getDigit(minValue);
        for (int i = 0;i<times;i++,mod*=10,dev*=10){
            int counter[][] =  new int[10*2][0];
            for (int num : nums) {
                int bucket = (num % mod) / dev + 10;
                counter[bucket] = AppendInt(counter[bucket], num);
            }
            int pos = 0;
            for (int[] aCounter : counter) {
                for (int anACounter : aCounter) {
                    nums[pos++] = anACounter;
                }
            }
        }
        return nums;
    }

    private int[] AppendInt(int[] array,int data){
        array = Arrays.copyOf(array,array.length+1);
        array[array.length-1] = data;
        return array;
    }

    private int getDigit(int num){
        if (num == 0){
            return 1;
        }
        int length = 0;
        for (;num!=0;num/=10){
            length++;
        }
        return length;
    }

    private int[] heapSort(int[] nums) {
        for (int i = (nums.length - 1) / 2; i >= 0; i--) {
            adjustHeap(nums, i, nums.length);
        }
        for (int i = nums.length - 1; i > 0; i--) {
            exchange(nums, 0, i);
            adjustHeap(nums, 0, i);
        }
        return nums;
    }

    private void adjustHeap(int[] nums, int start, int length) {
        int temp = nums[start];
        for (int i = start * 2 + 1; i < length; i = 2 * i + 1) {
            if (i + 1 < length && nums[i] < nums[i + 1]) {
                i++;
            }
            if (nums[i] > nums[(i - 1) / 2]) {
                exchange(nums, i, (i - 1) / 2);
                start = i;
            }
            else{
                break;
            }
        }
        nums[start] = temp;
    }

    private void exchange(int[] nums, int i, int j) {
        int temp = nums[i];
        nums[i] = nums[j];
        nums[j] = temp;
    }


    private int[] shellSort(int[] nums) {
        int gap = nums.length / 2;
        while (gap != 1) {
            for (int i = 0; i < gap; i++) {
                if (nums[i] > nums[i + gap]) {
                    exchange(nums, i, i + gap);
                }
            }
            gap /= 2;
        }
        return insertSort(nums);
    }

    private int[] bubbleSort(int[] data) {
        int temp;
        int count;
        for (int i = 0; i < data.length - 1; i++) {
            count = 0;
            for (int j = 0; j < data.length - i - 1; j++) {
                if (data[j] > data[j + 1]) {
                    temp = data[j];
                    data[j] = data[j + 1];
                    data[j + 1] = temp;
                    count++;
                }
            }
            if (count == 0) {
                return data;
            }
        }
        return data;
    }

    private int[] chooseSort(int[] data) {
        int minNum;
        int minIndex;
        int temp;
        for (int i = 0; i < data.length - 1; i++) {
            minNum = data[i];
            minIndex = i;
            for (int j = i + 1; j < data.length; j++) {
                if (data[j] < minNum) {
                    minNum = data[j];
                    minIndex = j;
                }
            }
            if (minIndex != i) {
                temp = data[minIndex];
                data[minIndex] = data[i];
                data[i] = temp;
            }
        }
        return data;
    }

    private int[] insertSort(int[] data) {
        int temp;
        for (int i = 1; i < data.length; i++) {
            for (int j = i; j > 0; j--) {
                if (data[j] < data[j - 1]) {
                    temp = data[j];
                    data[j] = data[j - 1];
                    data[j - 1] = temp;
                } else {
                    break;
                }
            }
        }
        return data;
    }
}
```
