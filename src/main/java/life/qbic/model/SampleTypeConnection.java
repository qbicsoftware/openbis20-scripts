package life.qbic.model;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.SampleType;
import java.util.Objects;

public class SampleTypeConnection {

  private SampleType parent;
  private SampleType child;

  public SampleTypeConnection(SampleType parentType, SampleType childType) {
    this.parent = parentType;
    this.child = childType;
  }

  public SampleTypeConnection(SampleType parentType) {
    this(parentType, null);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SampleTypeConnection)) {
      return false;
    }

    SampleTypeConnection that = (SampleTypeConnection) o;
    if(child == null || that.child == null) {
      boolean a = Objects.equals(child, that.child);
      if(parent == null || that.parent == null)  {
        return a && Objects.equals(parent, that.parent);
      } else {
        return a && Objects.equals(parent.getCode(), that.parent.getCode());
      }
    }
    if(parent == null || that.parent == null) {
      boolean a = Objects.equals(parent, that.parent);
      if(child == null || that.child == null)  {
        return a && Objects.equals(child, that.child);
      } else {
        return a && Objects.equals(child.getCode(), that.child.getCode());
      }
    }

    if (!Objects.equals(parent.getCode(), that.parent.getCode())) {
      return false;
    }
    return Objects.equals(child.getCode(), that.child.getCode());
  }

  @Override
  public int hashCode() {
    int result = parent != null ? parent.getCode().hashCode() : 0;
    result = 31 * result + (child != null ? child.getCode().hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    String parentCode = parent.getCode();
    if(child==null) {
      return parentCode;
    } else {
      return parentCode+" -> "+child.getCode();
    }
  }
}
