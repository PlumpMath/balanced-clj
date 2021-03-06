(ns balanced.core-test
  (:require [clojure.test     :refer :all]
            [vcr-clj.clj-http :refer [with-cassette]]
            [balanced.core    :refer :all]))

;; ===========================================================================
;; API Keys
;; ===========================================================================
(deftest test-api-keys
  (with-cassette :create-api-key do
    (let [[api-key _] (:api_keys (create-api-key))]
      (is (not-empty api-key))
      (is (= #{:links :created_at :secret :href :meta :id}
             (set (keys api-key))))
      (is (.startsWith (:secret api-key) "ak-test-"))
      (is (.startsWith (:href api-key)   "/api_keys")))))

(deftest test-fetch-api-key
  (with-cassette :fetch-api-key do
    (let [[new-api-key _] (:api_keys (create-api-key))
          [api-key _]     (:api_keys (fetch-api-key (:id new-api-key)))]
      (is (not-empty api-key))
      (is (= #{:href :created_at :meta :id :links}
             (set (keys api-key))))
      (is (= (dissoc new-api-key :secret) api-key)))))

(deftest test-list-api-keys
  (with-cassette :list-api-keys do
    (let [api-keys (list-api-keys)]
      (is (not-empty api-keys))
      (is (= #{:api_keys :links :meta}
             (set (keys api-keys))))
      (is (= 3 (count api-keys))))))

(deftest test-delete-api-key
  (with-cassette :delete-api-key do
    (let [[api-key _] (:api_keys (create-api-key))
          resp        (delete-api-key (:id api-key))]
      (is (not-empty resp))
      (is (= 204 (:status resp))))))

;; ===========================================================================
;; Customers
;; ===========================================================================
(deftest test-create-customer
  (with-cassette :create-customer do
    (let [[customer _] (:customers (create-customer))]
      (is (not-empty customer))
      (is (= #{:id :name :ssn_last4 :dob_year :dob_month
               :email :phone :address
               :business_name :ein :merchant_status
               :meta :href :links
               :created_at :updated_at}
             (set (keys customer)))))))

(deftest test-fetch-customer
  (with-cassette :fetch-customer do
    (let [[new-customer _] (:customers (create-customer))
          [customer _]     (:customers (fetch-customer (:id new-customer)))]
      (is (not-empty customer))
      (is (= #{:id :name :ssn_last4 :dob_year :dob_month
               :email :phone :address
               :business_name :ein :merchant_status
               :meta :href :links
               :created_at :updated_at}
             (set (keys customer))))
      (is (= (:id new-customer) (:id customer))))))

(deftest test-list-customers
  (with-cassette :list-customers do
    (let [customers (list-customers)]
      (is (not-empty customers))
      (is (= #{:customers :meta :links}
             (set (keys customers))))
      (is (= 6 (count (:customers customers)))))))

(deftest test-update-customer
  (with-cassette :update-customer do
    (let [[customer _]       (:customers (create-customer))
          [updated-customer] (:customers (update-customer (:id customer)
                                                          {:address
                                                           {:city  "Tulsa"
                                                            :state "OK"}}))]
      (is (= (:id customer) (:id updated-customer)))
      (is (= "Tulsa" (get-in updated-customer [:address :city])))
      (is (= "OK"    (get-in updated-customer [:address :state]))))))

(deftest test-delete-customer
  (with-cassette :delete-customer do
    (let [[customer _] (:customers (create-customer))
          resp         (delete-customer (:id customer))]
      (is (not-empty resp))
      (is (= 204 (:status resp))))))

;; ===========================================================================
;; Orders
;; ===========================================================================
(deftest test-create-order
  (with-cassette :create-order do
    (let [[customer _] (:customers (create-customer))
          [order _]    (:orders (create-order (:id customer)))]
      (is (not-empty order))
      (is (= #{:amount :amount_escrowed :currency :delivery_address
               :description
               :id :href :links :meta
               :created_at :updated_at}
             (set (keys order)))))))

(deftest test-fetch-order
  (with-cassette :fetch-order do
    (let [[customer _]  (:customers (create-customer))
          [new-order _] (:orders (create-order (:id customer)))
          [order _]     (:orders (fetch-order (:id new-order)))]
      (is (not-empty order))
      (is (= #{:amount :amount_escrowed :currency :delivery_address
               :description
               :id :href :links :meta
               :created_at :updated_at}))
      (is (= (:id order) (:id new-order))))))

(deftest test-list-orders
  (with-cassette :list-orders do
    (let [orders (list-orders)]
      (is (not-empty orders))
      (is (= #{:orders :links :meta}
             (set (keys orders))))
      (is (= 7 (count (:orders orders)))))))

(deftest test-update-order
  (with-cassette :update-order do
    (let [[customer _]      (:customers (create-customer))
          [order _]         (:orders (create-order (:id customer)))
          [updated-order _] (:orders (update-order (:id order)
                                                   {:description "Order #1"}))]
      (is (= (:id order) (:id updated-order)))
      (is (= "Order #1" (:description updated-order))))))

;; ===========================================================================
;; Bank Accounts
;; ===========================================================================
(def ^:dynamic bank-accounts
  {:invalid-1   {:routing_number "100000007"
                 :account_number "8887776665555"
                 :account_type   "checking"
                 :name           "John Q. Moneybags"}
   :invalid-2   {:routing_number "111111118"
                 :account_number "8887776665555"
                 :account_type   "checking"
                 :name           "John Q. Moneybags"}
   :pending-1   {:routing_number "021000021"
                 :account_number "9900000000"
                 :account_type   "checking"
                 :name           "John Q. Moneybags"}
   :pending-2   {:routing_number "321174851"
                 :account_number "9900000001"
                 :account_type   "checking"
                 :name           "John Q. Moneybags"}
   :succeeded-1 {:routing_number "021000021"
                 :account_number "9900000002"
                 :account_type   "checking"
                 :name           "John Q. Moneybags"}
   :succeeded-2 {:routing_number "321174851"
                 :account_number "9900000003"
                 :account_type   "checking"
                 :name           "John Q. Moneybags"}
   :failed-1    {:routing_number "021000021"
                 :account_number "9900000004"
                 :account_type   "checking"
                 :name           "John Q. Moneybags"}
   :failed-2    {:routing_number "321174851"
                 :account_number "9900000005"
                 :account_type   "checking"
                 :name           "John Q. Moneybags"}})

(deftest test-create-bank-account
  (with-cassette :create-bank-account do
    (let [[account _] (:bank_accounts
                       (create-bank-account (:succeeded-1 bank-accounts)))]
      (is (not-empty account))
      (is (= #{:account_number :account_type :address :bank_name
               :can_credit :can_debit :created_at :fingerprint :href
               :id :links :meta :name :routing_number :updated_at}
             (set (keys account)))))))

(deftest test-fetch-bank-account
  (with-cassette :fetch-bank-account do
    (let [[new-account _] (:bank_accounts
                           (create-bank-account (:succeeded-1 bank-accounts)))
          [account _]     (:bank_accounts
                           (fetch-bank-account (:id new-account)))]
      (is (not-empty account))
      (is (= #{:account_number :account_type :address :bank_name
               :can_credit :can_debit :created_at :fingerprint :href
               :id :links :meta :name :routing_number :updated_at}
             (set (keys account))))
      (is (= (:id account) (:id new-account))))))

(deftest test-list-bank-accounts
  (with-cassette :list-bank-accounts do
    (let [accounts (list-bank-accounts)]
      (is (not-empty accounts))
      (is (= #{:bank_accounts :links :meta}))
      (is (= 10 (count (:bank_accounts accounts)))))))

(deftest test-update-bank-account
  (with-cassette :update-bank-account do
    (let [[account _]         (:bank_accounts
                               (create-bank-account (:succeeded-1 bank-accounts)))
          [updated-account _] (:bank_accounts
                               (update-bank-account (:id account)
                                                    {:meta {:twitter.id         "1234567890"
                                                            :facebook.user_id   "0192837465"
                                                            :my-own-customer-id "12345"}}))]
      (is (= (:id account) (:id updated-account)))
      (is (= {:twitter.id         "1234567890"
              :facebook.user_id   "0192837465"
              :my-own-customer-id "12345"}
             (:meta updated-account))))))

(deftest test-delete-bank-account
  (with-cassette :delete-bank-account do
    (let [[account _] (:bank_accounts
                       (create-bank-account (:succeeded-1 bank-accounts)))
          resp             (delete-bank-account (:id account))]
      (is (not-empty resp))
      (is (= 204 (:status resp))))))

(deftest test-debit-bank-account
  (with-cassette :debit-bank-account do
    (let [[account _]      (:bank_accounts
                            (create-bank-account (:succeeded-1 bank-accounts)))
          [verification _] (:bank_account_verifications
                            (create-bank-account-verification (:id account)))
          [success _]      (:bank_account_verifications
                            (confirm-bank-account-verification (:id verification)
                                                               {:amount_1 1
                                                                :amount_2 1}))
          attrs            {:amount                  5000
                            :appears_on_statement_as "Statement text"
                            :description             "Some descriptive text for the debit in the dashboard"}
          [debit _]        (:debits
                            (debit-bank-account (:id account) attrs))]
      (is (not-empty debit))
      (is (= #{:amount :appears_on_statement_as :created_at :currency
               :description :failure_reason :failure_reason_code :href :id
               :links :meta :status :transaction_number :updated_at}
             (set (keys debit))))
      (is (= (:amount attrs)                 (:amount debit)))
      (is (= (:appears_on_statement_as attrs (str "BAL*" (:appears_on_statement_as debit)))))
      (is (= (:description attrs             (:description debit)))))))

(deftest test-associate-bank-account
  (with-cassette :associate-bank-account do
    (let [[customer _]         (:customers
                                (create-customer))
          [account _]          (:bank_accounts
                                (create-bank-account (:succeeded-1 bank-accounts)))
          [customer-account _] (:bank_accounts
                                (associate-bank-account (:id account) customer))]
      (is (not-empty account))
      (is (= #{:bank_account_verification :customer}
             (set (keys (:links customer-account)))))
      (is (= (:id customer) (get-in customer-account [:links :customer]))))))

;; ===========================================================================
;; Bank Account Verifications
;; ===========================================================================
(deftest test-create-bank-account-verification
  (with-cassette :create-bank-account-verification do
    (let [[account _]      (:bank_accounts
                            (create-bank-account (:pending-1 bank-accounts)))
          [verification _] (:bank_account_verifications
                            (create-bank-account-verification (:id account)))]
      (is (not-empty verification))
      (is (= 0 (:attempts verification)))
      (is (= 3 (:attempts_remaining verification)))
      (is (= "pending" (:verification_status verification)))
      (is (= (:id account) (get-in verification [:links :bank_account]))))))

(deftest test-fetch-bank-account-verification
  (with-cassette :fetch-bank-account-verification do
    (let [[account _]          (:bank_accounts
                                (create-bank-account (:pending-1 bank-accounts)))
          [new-verification _] (:bank_account_verifications
                                (create-bank-account-verification (:id account)))
          [verification _]     (:bank_account_verifications
                                (fetch-bank-account-verification (:id new-verification)))]
      (is (not-empty verification))
      (is (= new-verification verification)))))

(deftest test-confirm-bank-account-verification
  (with-cassette :confirm-bank-account-verification do
    (let [[account _]   (:bank_accounts
                         (create-bank-account (:pending-1 bank-accounts)))
          [pending _]   (:bank_account_verifications
                         (create-bank-account-verification (:id account)))
          [succeeded _] (:bank_account_verifications
                         (confirm-bank-account-verification (:id pending)
                                                            {:amount_1 1
                                                             :amount_2 1}))]
      (is (not-empty succeeded))
      (is (= "succeeded" (:verification_status succeeded))))))

;; ===========================================================================
;; Cards
;; ===========================================================================
(def ^:dynamic cards
  {:success-1          {:brand            "VISA"
                        :number           "4111111111111111"
                        :expiration_month 1
                        :expiration_year  2099
                        :cvv              "123"}
   :success-2          {:brand            "MasterCard"
                        :number           "5105105105105100"
                        :expiration_month 1
                        :expiration_year  2099
                        :cvv              "123"}
   :success-3          {:brand            "AMEX"
                        :number           "3411111111111111"
                        :expiration_month 1
                        :expiration_year  2099
                        :cvv              "1234"}
   :processor-failure  {:brand            "VISA"
                        :number           "4444444444444448"
                        :expiration_month 1
                        :expiration_year  2099
                        :cvv              "123"}
   :tokenization-error {:brand            "VISA"
                        :number           "4222222222222220"
                        :expiration_month 1
                        :expiration_year  2099
                        :cvv              "123"}
   :cvv-match-fail     {:brand            "MasterCard"
                        :number           "5112000200000002"
                        :expiration_month 1
                        :expiration_year  2099
                        :cvv              "200"}
   :cvv-unsupported    {:brand            "VISA"
                        :number           "4457000300000007"
                        :expiration_month 1
                        :expiration_year  2099
                        :cvv              "901"}})

(deftest test-create-card
  (with-cassette :create-card do
    (let [[card _] (:cards (create-card (:success-1 cards)))]
      (is (not-empty card))
      (is (= #{:address :avs_postal_match :avs_result :avs_street_match
               :brand :created_at :cvv :cvv_match :cvv_result
               :expiration_month :expiration_year :fingerprint
               :href :id :is_verified :links :meta :name :number :updated_at}
             (set (keys card)))))))

(deftest test-fetch-card
  (with-cassette :fetch-card do
    (let [[new-card _] (:cards (create-card (:success-1 cards)))
          [card _]     (:cards (fetch-card (:id new-card)))]
      (is (not-empty card))
      (is (= new-card card)))))

(deftest test-list-cards
  (with-cassette :list-cards do
    (let [[cards _] (:cards (list-cards))]
      (is (not-empty cards))
      (is (= 20 (count cards))))))

(deftest test-update-card
  (with-cassette :update-card do
    (let [[card _]         (:cards (create-card (:success-1 cards)))
          attrs            {:meta {:twitter.id         "1234567890"
                                   :facebook.user_id   "0192837465"
                                   :my-own-customer-id "12345"}}
          [updated-card _] (:cards (update-card (:id card) attrs))]
      (is (not-empty updated-card))
      (is (= (dissoc (merge card attrs) :updated_at)
             (dissoc updated-card :updated_at))))))

(deftest test-delete-card
  (with-cassette :delete-card do
    (let [[card _] (:cards (create-card (:success-1 cards)))
          resp     (delete-card (:id card))]
      (is (not-empty resp))
      (is (= 204 (:status resp))))))

(deftest test-debit-card
  (with-cassette :debit-card do
    (let [[card _]  (:cards (create-card (:success-1 cards)))
          attrs     {:amount                  5000
                     :appears_on_statement_as "Statement text"
                     :description             "Some descriptive text for the debit in the dashboard"}
          [debit _] (:debits (debit-card (:id card) attrs))]
      (is (not-empty debit))
      (is (= #{:amount :appears_on_statement_as :created_at :currency
               :description :failure_reason :failure_reason_code :href :id
               :links :meta :status :transaction_number :updated_at}
             (set (keys debit))))
      (is (= (:amount attrs)
             (:amount debit)))
      (is (= (str "BAL*" (:appears_on_statement_as attrs))
             (:appears_on_statement_as debit)))
      (is (= (:description attrs)
             (:description debit))))))

(deftest test-associate-card
  (with-cassette :associate-card do
    (let [[customer _]      (:customers (create-customer))
          [card _]          (:cards (create-card (:success-1 cards)))
          [customer-card _] (:cards (associate-card (:id card) customer))]
      (is (not-empty customer-card))
      (is (= #{:customer} (set (keys (:links customer-card)))))
      (is (= (:id customer) (get-in customer-card [:links :customer]))))))

;; ===========================================================================
;; Card Holds
;; ===========================================================================
(deftest test-create-card-hold
  (with-cassette :create-card-hold do
    (let [[card _]    (:cards
                       (create-card (:success-1 cards)))
          amount      5000
          description "Some descriptive text for the debit in the dashboard"
          [hold _]    (:card_holds
                       (create-card-hold (:id card)
                                         {:amount      amount
                                          :description description}))]
      (is (not-empty hold))
      (is (= amount      (:amount hold)))
      (is (= description (:description hold)))
      (is (= (:id card)  (get-in hold [:links :card]))))))

(deftest test-fetch-card-hold
  (with-cassette :fetch-card-hold do
    (let [[card _]     (:cards
                        (create-card (:success-1 cards)))
          amount       5000
          description  "Some descriptive text for the debit in the dashboard"
          [new-hold _] (:card_holds
                        (create-card-hold (:id card)
                                          {:amount      amount
                                           :description description}))
          [hold _]     (:card_holds
                        (fetch-card-hold (:id new-hold)))]
      (is (not-empty hold))
      (is (= new-hold hold)))))

(deftest test-list-card-holds
  (with-cassette :list-card-holds do
    (let [[card _] (:cards
                    (create-card (:success-1 cards)))
          [hold _] (:card_holds
                    (create-card-hold (:id card)
                                      {:amount      1
                                       :description "Just a buck"}))
          holds    (:card_holds (list-card-holds))]
      (is (not-empty holds))
      (is (> (count holds) 0))
      (is (some #{hold} holds)))))

(deftest test-update-card-hold
  (with-cassette :update-card-hold do
    (let [[card _]     (:cards
                        (create-card (:success-1 cards)))
          description  "old description"
          [new-hold _] (:card_holds
                        (create-card-hold (:id card)
                                          {:amount      5000
                                           :description description}))
          attrs        {:description "update this description"
                        :meta        {:holding.for    "user1"
                                      :meaningful.key "some.value"}}
          [hold _]     (:card_holds
                        (update-card-hold (:id new-hold) attrs))]
      (is (not-empty hold))
      (is (= (dissoc (merge new-hold attrs) :updated_at)
             (dissoc hold :updated_at))))))

;; TODO test-capture-card-hold

(deftest test-void-card-hold
  (with-cassette :void-card-hold do
    (let [[card _]     (:cards
                        (create-card (:success-1 cards)))
          [new-hold _] (:card_holds
                        (create-card-hold (:id card) {:amount 5000}))
          [hold _]     (:card_holds
                        (void-card-hold (:id new-hold)))]
      (is (not-empty hold))
      (is (not-empty (:voided_at hold))))))

;; ===========================================================================
;; Debits
;; ===========================================================================
(deftest test-fetch-debit
  (with-cassette :fetch-debit do
    (let [[card _]      (:cards (create-card (:success-1 cards)))
          attrs         {:amount                  5000
                         :appears_on_statement_as "Statement text"
                         :description             "Some descriptive text for the debit in the dashboard"}
          [new-debit _] (:debits (debit-card (:id card) attrs))
          [debit _]     (:debits (fetch-debit (:id new-debit)))]
      (is (not-empty debit))
      (is (= debit new-debit)))))

(deftest test-list-debits
  (with-cassette :list-debits do
    (let [[card _]  (:cards (create-card (:success-1 cards)))
          attrs     {:amount                  5000
                     :appears_on_statement_as "Statement text"
                     :description             "Some descriptive text for the debit in the dashboard"}
          [debit _] (:debits
                     (debit-card (:id card) attrs))
          debits    (:debits (list-debits))]
      (is (not-empty debits))
      (is (> (count debits) 0))
      (is (some #{debit} debits)))))

(deftest test-update-debit
  (with-cassette :update-debit do
    (let [[card _]      (:cards (create-card (:success-1 cards)))
          attrs         {:amount                  5000
                         :appears_on_statement_as "Statement text"
                         :description             "Some descriptive text for the debit in the dashboard"}
          [old-debit _] (:debits
                         (debit-card (:id card) attrs))
          update-attrs  {:description "New description for debit"
                         :meta        {:facebook.id "1234567890"
                                       :anykey      "valuegoeshere"}}
          [debit _]     (:debits
                         (update-debit (:id old-debit) update-attrs))]
      (is (not-empty debit))
      (is (= (dissoc (merge old-debit update-attrs) :updated_at)
             (dissoc debit :updated_at))))))

;; ===========================================================================
;; Credits
;; ===========================================================================
;; TODO test-fetch-credit

;; TODO test-list-credits (for all bank accounts, for a bank account)

;; TODO test-update-credit

;; ===========================================================================
;; Refunds (for Debits)
;; ===========================================================================
(deftest test-create-refund
  (with-cassette :create-refund do
    (let [[account _]      (:bank_accounts
                            (create-bank-account (:succeeded-1 bank-accounts)))
          [verification _] (:bank_account_verifications
                            (create-bank-account-verification (:id account)))
          [success _]      (:bank_account_verifications
                            (confirm-bank-account-verification (:id verification)
                                                               {:amount_1 1
                                                                :amount_2 1}))
          debit-attrs      {:amount                  5000
                            :appears_on_statement_as "Statement text"
                            :description             "Some descriptive text for the debit in the dashboard"}
          [debit _]        (:debits
                            (debit-bank-account (:id account) debit-attrs))
          refund-attrs     {:amount      5000
                            :description "Refund for Order #1111"
                            :meta        {:merchant.feedback          "positive"
                                          :user.refund_reason         "not happy with product"
                                          :fulfillment.item.condition "OK"}}
          [refund _]       (:refunds
                            (create-refund (:id debit) refund-attrs))]
      (is (not-empty refund))
      (is (= (:amount refund)                                    (:amount refund-attrs)))
      (is (= (:description refund)                               (:description refund-attrs)))
      (is (= (get-in refund [:meta :merchant.feedback])          (get-in refund-attrs [:meta :merchant.feedback])))
      (is (= (get-in refund [:meta :user.refund_reason])         (get-in refund-attrs [:meta :user.refund_reason])))
      (is (= (get-in refund [:meta :fulfillment.item.condition]) (get-in refund-attrs [:meta :fulfillment.item.condition])))
      (is (= (get-in refund [:links :debit])                     (:id debit))))))

(deftest test-fetch-refund
  (with-cassette :fetch-refund do
    (let [[account _]      (:bank_accounts
                            (create-bank-account (:succeeded-1 bank-accounts)))
          [verification _] (:bank_account_verifications
                            (create-bank-account-verification (:id account)))
          [success _]      (:bank_account_verifications
                            (confirm-bank-account-verification (:id verification)
                                                               {:amount_1 1
                                                                :amount_2 1}))
          debit-attrs      {:amount                  5000
                            :appears_on_statement_as "Statement text"
                            :description             "Some descriptive text for the debit in the dashboard"}
          [debit _]        (:debits
                            (debit-bank-account (:id account) debit-attrs))
          refund-attrs     {:amount      5000
                            :description "Refund for Order #1111"
                            :meta        {:merchant.feedback          "positive"
                                          :user.refund_reason         "not happy with product"
                                          :fulfillment.item.condition "OK"}}
          [new-refund _]   (:refunds
                            (create-refund (:id debit) refund-attrs))
          [refund _]       (:refunds
                            (fetch-refund (:id new-refund)))]
      (is (not-empty refund))
      (is (= (:id refund) (:id new-refund))))))

(deftest test-list-refunds
  (with-cassette :list-refunds do
    (let [[account _]      (:bank_accounts
                            (create-bank-account (:succeeded-1 bank-accounts)))
          [verification _] (:bank_account_verifications
                            (create-bank-account-verification (:id account)))
          [success _]      (:bank_account_verifications
                            (confirm-bank-account-verification (:id verification)
                                                               {:amount_1 1
                                                                :amount_2 1}))
          debit-attrs      {:amount                  5000
                            :appears_on_statement_as "Statement text"
                            :description             "Some descriptive text for the debit in the dashboard"}
          [debit _]        (:debits
                            (debit-bank-account (:id account) debit-attrs))
          refund-attrs     {:amount      5000
                            :description "Refund for Order #1111"
                            :meta        {:merchant.feedback          "positive"
                                          :user.refund_reason         "not happy with product"
                                          :fulfillment.item.condition "OK"}}
          [refund _]       (:refunds
                            (create-refund (:id debit) refund-attrs))
          refunds          (list-refunds)]
      (is (not-empty refunds))
      (is (= #{:refunds :links :meta}
             (set (keys refunds))))
      (is (> (count refunds) 0)))))

(deftest test-update-refund
  (with-cassette :update-refund do
    (let [[account _]      (:bank_accounts
                            (create-bank-account (:succeeded-1 bank-accounts)))
          [verification _] (:bank_account_verifications
                            (create-bank-account-verification (:id account)))
          [success _]      (:bank_account_verifications
                            (confirm-bank-account-verification (:id verification)
                                                               {:amount_1 1
                                                                :amount_2 1}))
          debit-attrs      {:amount                  5000
                            :appears_on_statement_as "Statement text"
                            :description             "Some descriptive text for the debit in the dashboard"}
          [debit _]        (:debits
                            (debit-bank-account (:id account) debit-attrs))
          old-refund-attrs {:amount      5000
                            :description "Refund for Order #1111"
                            :meta        {:merchant.feedback          "positive"
                                          :user.refund_reason         "not happy with product"
                                          :fulfillment.item.condition "OK"}}
          [old-refund _]   (:refunds
                            (create-refund (:id debit) old-refund-attrs))
          refund-attrs     {:description "update this description"
                            :meta        {:user.refund.count "3"
                                          :refund.reason     "user not happy with product"
                                          :user.notes        "very polite on the phone"}}
          [refund _]       (:refunds
                            (update-refund (:id old-refund) refund-attrs))]
      (is (not-empty refund))
      (is (= (:id refund)                               (:id old-refund)))
      (is (= (:description refund)                      (:description refund-attrs)))
      (is (= (get-in refund [:meta :user.refund.count]) (get-in refund-attrs [:meta :user.refund.count])))
      (is (= (get-in refund [:meta :refund.reason])     (get-in refund-attrs [:meta :refund.reason])))
      (is (= (get-in refund [:meta :user.notes])        (get-in refund-attrs [:meta :user.notes]))))))

;; ===========================================================================
;; Reversals (for Credits)
;; ===========================================================================
;; TODO test-create-reversal

;; TODO test-fetch-reversal

;; TODO test-list-reversals

;; TODO test-update-reversal

;; ===========================================================================
;; Events
;; ===========================================================================
(deftest test-fetch-event
  (with-cassette :fetch-event do
    (let [[first-event _] (:events (list-events))
          [event _]       (:events (fetch-event (:id first-event)))]
      (is (not-empty event))
      (is (= #{:callback_statuses :entity :href :id :links :occurred_at :type}
             (set (keys event))))
      (is (= (:id first-event) (:id event))))))

(deftest test-list-event
  (with-cassette :list-events do
    (let [events (list-events)]
      (is (not-empty events))
      (is (= #{:events :links :meta}
             (set (keys events))))
      (is (= (count events) 3)))))

;; ===========================================================================
;; Callbacks (for Events)
;; ===========================================================================
(deftest test-create-callback
  (with-cassette :create-callback do
    (let [url          "http://www.example.com/create-callback"
          [callback _] (:callbacks (create-callback {:url url}))]
      (is (not-empty callback))
      (is (= #{:href :id :links :method :revision :url}
             (set (keys callback))))
      (is (= url (:url callback))))))

(deftest test-fetch-callback
  (with-cassette :fetch-callback do
    (let [url              "http://www.example.com/fetch-callback"
          [new-callback _] (:callbacks (create-callback {:url url}))
          [callback _]     (:callbacks (fetch-callback (:id new-callback)))]
      (is (not-empty callback))
      (is (= #{:href :id :links :method :revision :url}
             (set (keys callback))))
      (is (= (:id callback) (:id new-callback))))))

(deftest test-list-callbacks
  (with-cassette :list-callbacks do
    (let [callbacks (list-callbacks)]
      (is (not-empty callbacks))
      (is (= #{:callbacks :links :meta}
             (set (keys callbacks))))
      (is (= (count callbacks) 3)))))

(deftest test-delete-callback
  (with-cassette :delete-callback do
    (let [url "http://www.example.com/delete-callback"
          [callback _] (:callbacks (create-callback {:url url}))
          resp (delete-callback (:id callback))]
      (is (not-empty resp))
      (is (= 204 (:status resp))))))
