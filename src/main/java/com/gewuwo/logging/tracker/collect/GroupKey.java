package com.gewuwo.logging.tracker.collect;

/**
 * 项目唯一标识
 *
 * <p>
 *     当前只有项目名称，后续会扩展字段
 * </p>
 *
 * @author gewuwo
 * @version 1.0
 * @since 2022/1/7 2:50 下午
 */
public class GroupKey {


    /**
     * 项目名
     */
    private final String project;

    private final String key;

    public GroupKey(String project) {
        this.project = project;
        this.key = project;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GroupKey groupKey = (GroupKey) o;

        return key.equals(groupKey.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return key;
    }

    public String getKey() {
        return key;
    }

    public String getProject() {
        return project;
    }

}
