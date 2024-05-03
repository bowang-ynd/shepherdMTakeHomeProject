package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.vo.request.AddCreditCardToUserPayload;
import com.shepherdmoney.interviewproject.vo.request.UpdateBalancePayload;
import com.shepherdmoney.interviewproject.vo.response.CreditCardView;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
public class CreditCardController {

    // TODO: wire in CreditCard repository here (~1 line)
    private final UserRepository userRepository;
    private final CreditCardRepository creditCardRepository;

    public CreditCardController(UserRepository userRepository, CreditCardRepository creditCardRepository){
        this.userRepository = userRepository;
        this.creditCardRepository = creditCardReposity;
    }

    @PostMapping("/credit-card")
    public ResponseEntity<Integer> addCreditCardToUser(@RequestBody AddCreditCardToUserPayload payload) {
        // TODO: Create a credit card entity, and then associate that credit card with user with given userId
        //       Return 200 OK with the credit card id if the user exists and credit card is successfully associated with the user
        //       Return other appropriate response code for other exception cases
        //       Do not worry about validating the card number, assume card number could be any arbitrary format and length

        // checks if user with given payload exists in the database
        Optional<User> optionalUser = userRepository.findById(payload.getUserId());
        if (optionalUser.isEmpty()){
            return new ResponseEntity<>(String.format("User with id: %d not found in database", payload.getUserId()), HttpStatus.NOT_FOUND);
        }
        User user = optionalUser.get();
        
        // checks if user already has this credit card 
        boolean creditCardExists = uset.getCreditCards().stream().anyMatch(creditCard -> creditCard.getNumber().equals(payload.getCardNumber()) && creditCard.getIssuanceBank().euqals(payload.getCardIssuanceBank()));

        if (creditCardExists){
            return new ResponseEntity<>("Credit card already associated with user.", HttpStatus.CONFLICT);
        }

        // if user is found, acquire it and create new credit card entity
        CreditCard creditCard = new CreditCard();
        creditCard.setIssuanceBank(payload.getCardIssuanceBank());
        creditCard.setNumber(payload.getCardNumber());
        creditCard.setOwner(user);

        // save credit card to the database 
        CreditCard savedCreditCard = creditCardRepository.save(creditCard);

        return new ResponseEntity<>(savedCreditCard.getId(), HttpStatus.OK);
    }

    @GetMapping("/credit-card:all")
    public ResponseEntity<List<CreditCardView>> getAllCardOfUser(@RequestParam int userId) {
        // TODO: return a list of all credit card associated with the given userId, using CreditCardView class
        //       if the user has no credit card, return empty list, never return null

        // checks if user with given payload exists in the database
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()){
            return new ResponseEntity<>(String.format("User with id: %d not found in database", userId), HttpStatus.NOT_FOUND);
        }
        User user = optionalUser.get();

        List<CreditCard> creditCards = creditCardRepository.findByOwner(user);

        List<CreditCardView> creditCardViews = creditCards.stream().map(this::mapToCreditCardView).collect(Collectors.toList());

        return new ResponseEntity<>(creditCardViews, HttpStatus.OK);
    }

    @GetMapping("/credit-card:user-id")
    public ResponseEntity<Integer> getUserIdForCreditCard(@RequestParam String creditCardNumber) {
        // TODO: Given a credit card number, efficiently find whether there is a user associated with the credit card
        //       If so, return the user id in a 200 OK response. If no such user exists, return 400 Bad Request

        Optional<CreditCard> optionalCreditCard = creditCardRepository.findByNumber(creditCardNumber);

        if (optionalCreditCard.isEmpty()){
            return new ResponseEntity<>(String.format("CreditCard with number: %s not found.", creditCardNumber), HttpStatus.BAD_REQUEST);
        } 

        CreditCard creditCard = optionalCreditCard.get();

        // check if the owner associated with this credit card exists
        User user = creditCard.getOwner();

        if (user == null){
            return new ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } else {
            return user.getId();
        }
    }

    @PostMapping("/credit-card:update-balance")
    public ResponseEntity<Void> updateBalance(@RequestBody UpdateBalancePayload[] payload) {
        //TODO: Given a list of transactions, update credit cards' balance history.
        //      1. For the balance history in the credit card
        //      2. If there are gaps between two balance dates, fill the empty date with the balance of the previous date
        //      3. Given the payload `payload`, calculate the balance different between the payload and the actual balance stored in the database
        //      4. If the different is not 0, update all the following budget with the difference
        //      For example: if today is 4/12, a credit card's balanceHistory is [{date: 4/12, balance: 110}, {date: 4/10, balance: 100}],
        //      Given a balance amount of {date: 4/11, amount: 110}, the new balanceHistory is
        //      [{date: 4/12, balance: 120}, {date: 4/11, balance: 110}, {date: 4/10, balance: 100}]
        //      This is because
        //      1. You would first populate 4/11 with previous day's balance (4/10), so {date: 4/11, amount: 100}
        //      2. And then you observe there is a +10 difference
        //      3. You propagate that +10 difference until today
        //      Return 200 OK if update is done and successful, 400 Bad Request if the given card number
        //        is not associated with a card.
        
        // iterate over each balance history
        for (UpdateBalancePayload UBPayload: payload){
            Optional<CreditCard> optionalCreditCard = creditCardRepository.findByNumber(UBPayload.getCreditCardNumber());
            if (optionalCreditCard.isEmpty()){
                return new ResponseEntity<>(String.format("Given credit card number: $s is not assiciated.", UBPayload.getCreditCardNumber()), HttpStatus.BAD_REQUEST);
            }
            CreditCard creditCard = optionalCreditCard.get();

            // update balance history with given date and amount
            updateBalanceHistory(creditCard.getBalanceHistory(), UBPayload.getBalanceDate(), UBPayload.getBalanceAmount());

            creditCardRepository.save(creditCard);
        }

        return new ResponseEntity<>("Balance Updated successfully.", HttpStatus.OK);
    }

    private void updateBalanceHistory(List<BalanceHistory> balanceHistory, LocalDate balanceDate, double balanceAmount){
        // sort the balanceHisotry list by date in ascending order
        balanceHistory.sort(Comparator.comparing(BalanceHistory::getDate));

        // find the index of the balance history entry with the given date, or the nearest previous date
        int idx = 0;
        while (idx < balanceHistory.size() && balanceHistory.get(idx).getDate().isBefore(balanceDate)){
            idx++;
        }

        // if there is no balance history entry with given date, move to its nearest previous date
        if (idx == balanceHistory.size() || !balanceHistory.get(idx).getDate().isEqual(balanceDate)){
            idx = idx - 1 < 0 ? 0 : idx - 1;
        }

        // if there is no entry at the given date, insert a new one 
        if (idx == balanceHistory.size() && balanceHistory.get(idx).getDate().isEqual(balanceDate)){
            balanceHistory.get(idx).setBalance(balanceAmount);
        } else { 
            BalanceHistory newBalanceHistory = new BalanceHistory();
            newBalanceHistory.setDate(balanceDate);
            newBalanceHistory.setBalance(balanceAmount);
            balanceHistory.add(idx + 1, balanceHistory);
        }

        // propagate balance difference to following entries
        double difference = balanceAmount - balanceHistory.get(idx).getBalance();
        for (int i = idx + 1; i < balanceHistory.size(); i++){
            balanceHistory.get(idx).setBalance(balanceHistory.get(idx).getBalance() + difference);
        }
    }
    
}
