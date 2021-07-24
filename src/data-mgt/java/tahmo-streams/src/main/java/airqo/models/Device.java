package airqo.models;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;


@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Device implements Serializable {

    @JsonAlias({ "device_name", "deviceName" })
    private String name = "";

    @JsonAlias({ "id", "device_id" })
    private String _id = "";

    @JsonAlias({ "channel_id", "channelID" })
    private int device_number;

    @JsonAlias({ "site_details", "siteDetails" })
    private SiteDetails site = new SiteDetails();

    @Override
    public String toString() {
        return "Device{" +
                "name='" + name + '\'' +
                ", _id='" + _id + '\'' +
                ", site=" + site +
                '}';
    }
}
