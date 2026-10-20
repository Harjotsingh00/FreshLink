package freshlink;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Clearly fictional listings for first-run demonstration. */
public final class DemoData {
    private DemoData() { }
    public static List<FoodItem> create() {
        LocalDate d = LocalDate.now(); List<FoodItem> a = new ArrayList<>();
        add(a,"Sourdough bread packets","Bakery",20,"packets",d.plusDays(1),"Sunrise Bakehouse","18 Market Lane","Baked this morning.");
        add(a,"Butter croissants","Bakery",12,"pieces",d,"Sunrise Bakehouse","18 Market Lane","Collect before closing.");
        add(a,"Mixed pastry trays","Bakery",3,"trays",d.plusDays(2),"Sunrise Bakehouse","18 Market Lane","Assorted pastries.");
        add(a,"Carrot and potato boxes","Produce",8,"boxes",d.plusDays(2),"Green Valley Farms","4 Willow Street","Locally grown.");
        add(a,"Ripe tomatoes","Produce",14,"kg",d.plusDays(1),"Green Valley Farms","Farm Gate, North Road","Great for sauces.");
        add(a,"Fresh apples","Produce",10,"kg",d.plusDays(5),"Green Valley Farms","Farm Gate, North Road","Mixed varieties.");
        add(a,"Whole milk bottles","Dairy",16,"litres",d.plusDays(1),"Pure Moo Dairy","72 Orchard Road","Keep refrigerated.");
        add(a,"Plain yoghurt tubs","Dairy",12,"pieces",d.plusDays(3),"Pure Moo Dairy","72 Orchard Road","Sealed 500 g tubs.");
        add(a,"Cheddar portions","Dairy",6,"packets",d.plusDays(6),"Pure Moo Dairy","72 Orchard Road","Chilled.");
        add(a,"Vegetable curry meals","Prepared meals",18,"portions",d,"Campus Kitchen","Campus Hall, east entrance","Chilled and labelled.");
        add(a,"Lentil soup","Prepared meals",10,"litres",d.plusDays(1),"Campus Kitchen","Campus Hall, east entrance","Bring clean containers.");
        add(a,"Rice and bean bowls","Prepared meals",14,"portions",d.plusDays(2),"Campus Kitchen","Campus Hall, east entrance","Vegetarian.");
        add(a,"Brown lentils","Pantry",10,"packets",d.plusDays(90),"Neighbourhood Mart","2 Cedar Avenue","Unopened 1 kg packs.");
        add(a,"Canned chopped tomatoes","Pantry",24,"pieces",d.plusDays(120),"Neighbourhood Mart","2 Cedar Avenue","Store cupboard surplus.");
        add(a,"Oats","Pantry",8,"boxes",d.plusDays(150),"Neighbourhood Mart","2 Cedar Avenue","Sealed packs.");
        add(a,"Fresh orange juice","Beverage",8,"litres",d.plusDays(1),"Fresh Sip Café","9 Station Square","Chilled.");
        add(a,"Frozen vegetable mix","Frozen",12,"packets",d.plusDays(45),"ColdChain Foods","ColdChain Depot, Unit 4","Keep frozen.");
        a.add(FoodItem.create("Frozen berry packs","Frozen",9,"packets",d.plusDays(60),"ColdChain Foods","ColdChain Depot, Unit 4","Fictional demo; reserved for Hope Shelter.").claimedBy("Hope Shelter"));
        a.add(FoodItem.create("Apple oat muffins","Bakery",10,"pieces",d.minusDays(1),"Sunrise Bakehouse","18 Market Lane","Fictional demo; already collected.").claimedBy("City Community Kitchen"));
        return a;
    }
    private static void add(List<FoodItem> a,String n,String c,int q,String u,LocalDate d,String donor,String loc,String notes) {
        boolean urgent=!d.isAfter(LocalDate.now());
        String phone=donor.contains("Farm")?"+1 555 010 2020":donor.contains("Dairy")?"+1 555 010 3030":"+1 555 010 1010";
        a.add(FoodItem.create(n,c,q,u,d,donor,loc,"Fictional demo listing. " + notes,phone,LocalTime.of(14,0),LocalTime.of(17,0),urgent?"Urgent":"Normal"));
    }
}
