package org.hascoapi.entity.pojo;

public interface SlotListElement {

    /*
     *  ContainerSlot and Subcontainers are slot elements of containers.
     */

    public String getUri();

    public String getHasNext();

    public void setHasNext(String hasNext);

    public String getHasPrevious();

    public void setHasPrevious(String hasPrevious);

    public String getBelongsTo();

    public void save();

}
