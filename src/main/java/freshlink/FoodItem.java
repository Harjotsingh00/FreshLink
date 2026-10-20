package freshlink;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Immutable food rescue listing, including pickup and workflow details. */
public final class FoodItem {
    private final String id,name,category,unit,donor,location,notes,claimedBy,reservedBy,contactNumber,priority;
    private final int quantity;
    private final LocalDate expiryDate;
    private final LocalTime pickupFrom,pickupUntil;

    public FoodItem(String id,String name,String category,int quantity,String unit,LocalDate expiryDate,String donor,String location,String notes,String claimedBy){
        this(id,name,category,quantity,unit,expiryDate,donor,location,notes,claimedBy,"");
    }
    public FoodItem(String id,String name,String category,int quantity,String unit,LocalDate expiryDate,String donor,String location,String notes,String claimedBy,String reservedBy){
        this(id,name,category,quantity,unit,expiryDate,donor,location,notes,claimedBy,reservedBy,"",null,null,"Normal");
    }
    public FoodItem(String id,String name,String category,int quantity,String unit,LocalDate expiryDate,String donor,String location,String notes,
                    String claimedBy,String reservedBy,String contactNumber,LocalTime pickupFrom,LocalTime pickupUntil,String priority){
        this.id=id==null||id.isBlank()?UUID.randomUUID().toString():id;this.name=name==null?"":name;this.category=category==null?"Other":category;
        this.quantity=quantity;this.unit=unit==null?"pieces":unit;this.expiryDate=expiryDate;this.donor=donor==null?"":donor;this.location=location==null?"":location;
        this.notes=notes==null?"":notes;this.claimedBy=claimedBy==null?"":claimedBy;this.reservedBy=reservedBy==null?"":reservedBy;
        this.contactNumber=contactNumber==null?"":contactNumber;this.pickupFrom=pickupFrom;this.pickupUntil=pickupUntil;this.priority=priority==null?"Normal":priority;
    }
    public static FoodItem create(String name,String category,int quantity,String unit,LocalDate expiryDate,String donor,String location,String notes){
        return create(name,category,quantity,unit,expiryDate,donor,location,notes,"",null,null,"Normal");
    }
    public static FoodItem create(String name,String category,int quantity,String unit,LocalDate expiryDate,String donor,String location,String notes,String contact,LocalTime from,LocalTime until,String priority){
        return new FoodItem(null,name,category,quantity,unit,expiryDate,donor,location,notes,"","",contact,from,until,priority);
    }
    public FoodItem claimedBy(String person){return new FoodItem(id,name,category,quantity,unit,expiryDate,donor,location,notes,person,"",contactNumber,pickupFrom,pickupUntil,priority);}
    public FoodItem reservedBy(String person){return new FoodItem(id,name,category,quantity,unit,expiryDate,donor,location,notes,claimedBy,person,contactNumber,pickupFrom,pickupUntil,priority);}
    public String getId(){return id;}public String getName(){return name;}public String getCategory(){return category;}public int getQuantity(){return quantity;}public String getUnit(){return unit;}
    public LocalDate getExpiryDate(){return expiryDate;}public String getDonor(){return donor;}public String getLocation(){return location;}public String getNotes(){return notes;}
    public String getClaimedBy(){return claimedBy;}public String getReservedBy(){return reservedBy;}public String getContactNumber(){return contactNumber;}
    public LocalTime getPickupFrom(){return pickupFrom;}public LocalTime getPickupUntil(){return pickupUntil;}public String getPriority(){return priority;}
    public boolean isUrgent(){return "Urgent".equalsIgnoreCase(priority);}
    public boolean isReserved(){return !reservedBy.isBlank()&&!isClaimed();}public boolean isClaimed(){return !claimedBy.isBlank();}
    public boolean isExpired(){return expiryDate!=null&&expiryDate.isBefore(LocalDate.now());}
    public boolean expiresSoon(){return expiryDate!=null&&!isExpired()&&!isClaimed()&&!expiryDate.isAfter(LocalDate.now().plusDays(2));}
}
