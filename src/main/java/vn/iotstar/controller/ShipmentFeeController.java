package vn.iotstar.controller;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.iotstar.service.impl.ShipmentFeeService;
@Controller
public class ShipmentFeeController {
    private final ShipmentFeeService fees;
    public ShipmentFeeController(ShipmentFeeService fees){this.fees=fees;}
    @GetMapping("/user/shipment-fees/{shipmentId}") public String page(@PathVariable Long shipmentId,Model model){
        model.addAttribute("fees",fees.view(shipmentId));return "user/shipment-fees";
    }
    @PostMapping("/user/shipment-fees/{shipmentId}/wallet") public String wallet(@PathVariable Long shipmentId,@RequestParam String obligation){
        pay(shipmentId,obligation,"E_WALLET",false);return "redirect:/user/shipment-fees/"+shipmentId;
    }
    @PostMapping("/user/shipment-fees/{shipmentId}/demo-vnpay") public String demo(@PathVariable Long shipmentId,@RequestParam String obligation,@RequestParam boolean confirmDemo){
        pay(shipmentId,obligation,"VNPAY",confirmDemo);return "redirect:/user/shipment-fees/"+shipmentId;
    }
    @PostMapping({"/hub/return-collection/cash","/shipper/return-tasks/cash"}) public String cash(@RequestParam Long shipmentId){
        fees.payReturn(shipmentId,"CASH",false);return "redirect:/";
    }
    private void pay(Long id,String obligation,String method,boolean demo){
        if("RETURN".equals(obligation))fees.payReturn(id,method,demo);
        else if("WEIGHT_SURCHARGE".equals(obligation))fees.paySurcharge(id,method,demo);
        else throw new IllegalArgumentException("Unknown fee obligation");
    }
}
