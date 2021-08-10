import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @Classname TreeUtils
 * @Description TODO
 * @Date 2021/8/10 21:37
 * @Author Bruce.Z
 */
public class TreeUtils {

    /**
     * 平铺树
     *
     * @param source             数据源
     * @param target             目标容器
     * @param childListFn        子节点集合方法
     * @param addTargetCondition 添加到容器的判断方法
     * @param <F>
     */
    public static <F> void treeToListDeep (List<F> source, List<F> target, Function<F, List<F>> childListFn, Predicate<F> addTargetCondition) {
        loopTree(source, childListFn, (x) -> {
            if (addTargetCondition.test(x)) {
                target.add(x);
            }
        });
    }

    /**
     * 平铺树,返回平铺结果
     *
     * @param source             数据源
     * @param childListFn        子节点集合方法
     * @param addTargetCondition 添加到容器的判断方法
     * @param <F>
     * @return 返回平铺后的list
     */
    public static <F> List<F> treeToListDeep (List<F> source, Function<F, List<F>> childListFn, Predicate<F> addTargetCondition) {
        List<F> target = new ArrayList<>();
        treeToListDeep(source, target, childListFn, addTargetCondition);
        return target;
    }

    /**
     * list转树
     *
     * @param source        数据源
     * @param childListFn   子节点集合方法
     * @param idFn          获取ID的方法
     * @param pidFn         获取父ID的方法
     * @param rootCondition 根节点的条件
     * @param <F>
     * @param <T>
     * @return
     */
    public static <F, T> List<F> listToTree (List<F> source, BiConsumer<F, List<F>> childListFn, Function<F, T> idFn, Function<F, T> pidFn, Predicate<F> rootCondition) {
        return listToTree(source, childListFn, idFn, pidFn, rootCondition, null);
    }

    /**
     * list转树, 带后置处理回调函数
     * (idx, obj) -> {System.out.println(obj);} , idx是从0开始的层级
     *
     * @param source        数据源流
     * @param childListFn   子节点集合方法
     * @param idFn          获取ID的方法
     * @param pidFn         获取父ID的方法
     * @param rootCondition 根节点的条件
     * @param listen        后置处理,回调函数
     * @param <F>
     * @param <T>
     * @return
     */
    public static <F, T> List<F> streamToTree (Stream<F> source, BiConsumer<F, List<F>> childListFn, Function<F, T> idFn, Function<F, T> pidFn, Predicate<F> rootCondition, BiConsumer<Integer, F> listen) {
        List<F> tree = new ArrayList<>();
        Map<T, List<F>> map = new ConcurrentHashMap<>();
        source.forEach(f -> {
            if (rootCondition.test(f)) {
                tree.add(f);
            } else {
                List<F> tempList = map.getOrDefault(pidFn.apply(f), new ArrayList<>());
                tempList.add(f);
                map.put(pidFn.apply(f), tempList);
            }
        });
        tree.forEach(x -> assembleTree(x, map, childListFn, idFn, listen, 0));
        return tree;
    }

    /**
     * list转树, 带后置处理回调函数
     * (idx, obj) -> {System.out.println(obj);} , idx是从0开始的层级
     *
     * @param source        数据源
     * @param childListFn   子节点集合方法
     * @param idFn          获取ID的方法
     * @param pidFn         获取父ID的方法
     * @param rootCondition 根节点的条件
     * @param listen        后置处理,回调函数
     * @param <F>
     * @param <T>
     * @return
     */
    public static <F, T> List<F> listToTree (List<F> source, BiConsumer<F, List<F>> childListFn, Function<F, T> idFn, Function<F, T> pidFn, Predicate<F> rootCondition, BiConsumer<Integer, F> listen) {
        return streamToTree(source.parallelStream(), childListFn, idFn, pidFn, rootCondition, listen);
    }

    /**
     * 组装树
     *
     * @param current     当前节点
     * @param map         所有数据源根据pid分组
     * @param childListFn 子节点集合方法
     * @param idFn        获取id的方法
     * @param listen      后置监听回调函数
     * @param idx         层级,从0开始
     * @param <F>
     * @param <T>
     */
    private static <F, T> void assembleTree (F current, Map<T, List<F>> map, BiConsumer<F, List<F>> childListFn, Function<F, T> idFn, BiConsumer<Integer, F> listen, int idx) {
        List<F> fs = map.get(idFn.apply(current));
        childListFn.accept(current, fs);
        if (!isEmpty(fs)) {
            fs.forEach(x -> assembleTree(x, map, childListFn, idFn, listen, idx + 1));
        }
        if (listen != null) {
            listen.accept(idx, current);
        }
    }

    private static <F> void loopTree (List<F> source, Function<F, List<F>> childListFn, Consumer<F> preListen) {
        if (isEmpty(source)) {
            return;
        }
        source.forEach(x -> {
            preListen.accept(x);
            loopTree(childListFn.apply(x), childListFn, preListen);
        });
    }

    /**
     * 遍历树
     *
     * @param source      资源
     * @param childListFn 子方法
     * @param preListen   前置监听
     * @param postListen  后置监听,子集为空返回false, 其他情况自行返回true, 子集全部返回true时父级返回true
     * @param <F>
     */
    public static <F> void loopTree (List<F> source, Function<F, List<F>> childListFn, Consumer<F> preListen, Consumer<F> postListen) {
        if (isEmpty(source)) {
            return;
        }
        source.forEach(x -> {
            preListen.accept(x);
            loopTree(childListFn.apply(x), childListFn, preListen, postListen);
            postListen.accept(x);
        });
    }

    public static <F> boolean isEmpty (List<F> source) {
        return source == null || source.isEmpty();
    }

}
