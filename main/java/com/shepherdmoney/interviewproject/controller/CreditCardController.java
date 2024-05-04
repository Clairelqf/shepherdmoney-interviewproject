package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.model.CreditCard;
import com.shepherdmoney.interviewproject.model.User;
import com.shepherdmoney.interviewproject.repository.CreditCardRepository;
import com.shepherdmoney.interviewproject.repository.UserRepository;

import com.shepherdmoney.interviewproject.vo.request.AddCreditCardToUserPayload;
import com.shepherdmoney.interviewproject.vo.request.UpdateBalancePayload;
import com.shepherdmoney.interviewproject.vo.response.CreditCardView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;

@RestController
public class CreditCardController {

    // TODO: wire in CreditCard repository here (~1 line)
    @Autowired
    private CreditCardRepository creditCardRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/credit-card")
    public ResponseEntity<Integer> addCreditCardToUser(@RequestBody AddCreditCardToUserPayload payload) {
        // TODO: Create a credit card entity, and then associate that credit card with user with given userId
        //       Return 200 OK with the credit card id if the user exists and credit card is successfully associated with the user
        //       Return other appropriate response code for other exception cases
        //       Do not worry about validating the card number, assume card number could be any arbitrary format and length
        Optional<User> optionalUser = userRepository.findById(payload.getUserId());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            CreditCard creditCard = new CreditCard();
            creditCard.setIssuanceBank(payload.getCardIssuanceBank());
            creditCard.setNumber(payload.getCardNumber());
            creditCard.setOwner(user);
            creditCardRepository.save(creditCard);
            return ResponseEntity.ok(creditCard.getId());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/credit-card:all")
    public ResponseEntity<List<CreditCardView>> getAllCardOfUser(@RequestParam int userId) {
        // TODO: return a list of all credit card associated with the given userId, using CreditCardView class
        //       if the user has no credit card, return empty list, never return null
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isPresent()) {
            List<CreditCard> creditCards = optionalUser.get().getCreditCards();
            List<CreditCardView> creditCardViews = creditCards.stream()
                    .map(creditCard -> CreditCardView.builder()
                            .issuanceBank(creditCard.getIssuanceBank())
                            .number(creditCard.getNumber())
                            .build())
                    .collect(Collectors.toList());
            return ResponseEntity.ok(creditCardViews);
        } else {
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    @GetMapping("/credit-card:user-id")
    public ResponseEntity<Integer> getUserIdForCreditCard(@RequestParam String creditCardNumber) {
        // TODO: Given a credit card number, efficiently find whether there is a user associated with the credit card
        //       If so, return the user id in a 200 OK response. If no such user exists, return 400 Bad Request
        Optional<CreditCard> optionalCreditCard = creditCardRepository.findByNumber(creditCardNumber);
        if (optionalCreditCard.isPresent()) {
            return ResponseEntity.ok(optionalCreditCard.get().getOwner().getId());
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/credit-card:update-balance")
    public ResponseEntity<String> updateBalance(@RequestBody UpdateBalancePayload[] payload) {
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
        for (UpdateBalancePayload transaction : payload) {
            // Find the credit card associated with the transaction's credit card number
            Optional<CreditCard> optionalCreditCard = creditCardRepository.findByNumber(transaction.getCreditCardNumber());
            if (optionalCreditCard.isPresent()) {
                CreditCard creditCard = optionalCreditCard.get();
                // Get or create the balance history for the credit card
                TreeMap<LocalDate, Double> balanceHistory = creditCard.getBalanceHistory();
                if (balanceHistory == null) {
                    balanceHistory = new TreeMap<>();
                    creditCard.setBalanceHistory(balanceHistory);
                }
                // Update the balance history with the transaction's balance amount for the transaction's date
                balanceHistory.put(transaction.getBalanceDate(), transaction.getBalanceAmount());
                // Fill in any gaps in the balance history with the balance of the previous date
                fillBalanceHistoryGaps(balanceHistory);
                // Update all following balances with the difference if needed
                updateFollowingBalances(balanceHistory);
                // Save the updated credit card
                creditCardRepository.save(creditCard);
            }
        }
        return ResponseEntity.ok().body("Update successful");
    }

    private void fillBalanceHistoryGaps(TreeMap<LocalDate, Double> balanceHistory) {
        LocalDate currentDate = LocalDate.now();
        LocalDate previousDate = currentDate.minusDays(1);
        while (!balanceHistory.containsKey(previousDate) && !previousDate.isBefore(balanceHistory.firstKey())) {
            balanceHistory.put(previousDate, balanceHistory.get(balanceHistory.lowerKey(previousDate)));
            previousDate = previousDate.minusDays(1);
        }
    }

    // Method to update all following balances with the difference if needed
    private void updateFollowingBalances(TreeMap<LocalDate, Double> balanceHistory) {
        Map.Entry<LocalDate, Double> latestEntry = balanceHistory.firstEntry();
        Double latestBalance = latestEntry.getValue();
        for (Map.Entry<LocalDate, Double> entry : balanceHistory.entrySet()) {
            if (!entry.getKey().equals(latestEntry.getKey())) {
                Double balance = entry.getValue();
                if (!balance.equals(latestBalance)) {
                    balanceHistory.put(entry.getKey(), latestBalance);
                }
            }
            latestBalance = entry.getValue();
        }
    }
    
}
