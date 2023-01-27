//const functions = require('firebase-functions');
const express = require("express");
const app = express();
const { resolve } = require("path");
// This is your real test secret API key.
const stripe = require("stripe")("sk_test_51LYI5uBj0L1UEhAKPtUWjWdE8y1BEtB2tOu3cCKrHQZBYHF9pErOWtmuHWPhEh8kkc3aFrEd72myE4QvyDhWcNvu00WgOXzweh");
app.use(express.static("."));
app.use(express.json());
const calculateOrderAmount = items => {
  // Replace this constant with a calculation of the order's amount
  // Calculate the order total on the server to prevent
  // people from directly manipulating the amount on the client
  console.log(items[0].amount)
  return items[0].amount;
};
app.post("/create-payment-intent", async (req, res) => {
  const { items } = req.body;
  // Create a PaymentIntent with the order amount and currency
  const paymentIntent = await stripe.paymentIntents.create({
    amount: calculateOrderAmount(items),
    currency: "GBP"
  });
  res.send({
    clientSecret: paymentIntent.client_secret
  });
});
app.get('/greet',(req,res)=>{
	res.send("It is working fine")
})
app.listen(4242, () => console.log('Node server listening on port 4242!'));