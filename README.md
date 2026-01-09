# WhatsApp Barbershop Booking System

## Overview
A dead-simple WhatsApp booking system built with Spring Boot: customers check if there's availability **today**. If fully booked, they can book for **tomorrow**. That's it.

**Project Name:** barbershop-booking
**Package:** com.banda.barbershop
**Main Class:** BarbershopBookingApplication

## The Problem
Customer calls: *"Any chance I can get a haircut today?"*
- 3 barbers on the phone with clients
- Receptionist doesn't know the schedule
- Customer hangs up, goes to competitor

## The Solution
**"Text us on WhatsApp, we'll tell you if we have slots today or tomorrow"**

Simple. Fast. No learning curve.

---

## MVP Features (Absolute Minimum)

### Customer Journey
1. Text "Hi" to barbershop WhatsApp
2. Choose service (Standard Cut, Fade, Trim, etc.)
3. System shows available times **today**
4. If fully booked today → shows times **tomorrow**
5. Customer picks a time
6. Instant confirmation with booking code

**That's it. No date pickers. No "next week" bookings. Just today/tomorrow.**

### What Happens Behind the Scenes
- System calculates available slots based on shop capacity
- Tracks bookings in database
- Prevents double-booking
- Customer can view/cancel their booking

---

## Sample User Flow

### Happy Path (Availability Today)
```
Customer: Hi
Bot: 👋 Welcome to Fade Factory Barbershop!

What service do you need?

1️⃣ Standard Cut - €25 (30 min)
2️⃣ Skin Fade - €30 (45 min)
3️⃣ Beard Trim - €15 (20 min)
4️⃣ Cut & Beard - €40 (60 min)

Customer: 2
Bot: 🪒 Skin Fade - checking availability...

📅 TODAY (Thu 09 Jan):
1️⃣ 2:30 PM
2️⃣ 4:00 PM
3️⃣ 5:30 PM

Type number to book or reply MORE for tomorrow

Customer: 1
Bot: Perfect! Confirm your booking:

🪒 Skin Fade
📅 TODAY at 2:30 PM
⏱️ 45 minutes
💰 €30
📍 123 Main St, Dublin

Reply YES to confirm or CANCEL to restart

Customer: YES
Bot: ✅ CONFIRMED!

Booking Code: #BK847

🪒 Skin Fade
📅 Thu 09 Jan at 2:30 PM
📍 123 Main St, Dublin

See you soon! 👍

To cancel: reply CANCEL #BK847
To view bookings: reply MY BOOKINGS
```

### If Today Is Fully Booked
```
Customer: 2
Bot: 🪒 Skin Fade - checking availability...

❌ Sorry, we're fully booked TODAY

📅 TOMORROW (Fri 10 Jan):
1️⃣ 10:00 AM
2️⃣ 11:30 AM
3️⃣ 2:00 PM
4️⃣ 3:30 PM
5️⃣ 5:00 PM

Type number to book

Customer: 4
Bot: Perfect! Confirm your booking:

🪒 Skin Fade
📅 TOMORROW at 3:30 PM
⏱️ 45 minutes
💰 €30
📍 123 Main St, Dublin

Reply YES to confirm or CANCEL to restart
```

---

## System Architecture

### High-Level Components

```
┌─────────────────┐
│   Customer      │
│   (WhatsApp)    │
└────────┬────────┘
         │
         │ Sends message
         ▼
┌─────────────────────────────────┐
│   Twilio WhatsApp API           │
│   (Webhook Handler)             │
└────────┬────────────────────────┘
         │
         │ HTTP POST
         ▼
┌─────────────────────────────────┐
│   Spring Boot Application       │
│                                 │
│  ┌─────────────────────┐       │
│  │ WhatsAppController  │       │
│  │ (Webhook Endpoint)  │       │
│  └──────────┬──────────┘       │
│             │                   │
│             ▼                   │
│  ┌─────────────────────┐       │
│  │ ConversationManager │       │
│  │ (State Machine)     │       │
│  └──────────┬──────────┘       │
│             │                   │
│    ┌────────┴────────┐         │
│    ▼                 ▼         │
│  ┌──────────┐   ┌──────────┐  │
│  │ Booking  │   │Available │  │
│  │ Service  │   │Service   │  │
│  └────┬─────┘   └─────┬────┘  │
│       │               │        │
│       ▼               ▼        │
│  ┌─────────────────────────┐  │
│  │  Database (MySQL)       │  │
│  │  - bookings             │  │
│  │  - services             │  │
│  │  - conversation_states  │  │
│  └─────────────────────────┘  │
└─────────────────────────────────┘
         │
         │ WhatsApp response
         ▼
┌─────────────────┐
│   Customer      │
│   (WhatsApp)    │
└─────────────────┘
```

---

## Core Components & Responsibilities

### 1. WhatsAppController
**Purpose**: Entry point for all WhatsApp messages

**Responsibilities**:
- Receives webhook POST requests from Twilio
- Extracts customer phone number and message text
- Delegates to ConversationManager
- Returns properly formatted WhatsApp response

**Key Decision**: Routes each incoming message to the state machine

---

### 2. ConversationManager
**Purpose**: State machine that manages conversation flow

**Responsibilities**:
- Retrieves current conversation state from database
- Determines what step customer is on (service selection, time selection, confirmation, etc.)
- Processes user input based on current step
- Updates conversation state
- Generates appropriate response message
- Transitions to next step

**Key Decisions**:
- Uses enum-based state tracking for conversation flow
- Stores conversation context in database as JSON
- Handles invalid inputs gracefully
- Provides clear instructions at each step

**State Transitions**:
```
MAIN_MENU → SELECT_SERVICE → VIEW_TODAY_SLOTS → CONFIRM_BOOKING → BOOKING_CONFIRMED
                                    ↓ (if fully booked today)
                              VIEW_TOMORROW_SLOTS → CONFIRM_BOOKING → BOOKING_CONFIRMED
```

---

### 3. AvailabilityService
**Purpose**: Calculate available time slots

**Responsibilities**:
- Generate all possible time slots for a given date
- Check shop configuration (opening hours, number of barbers)
- Query existing bookings for that date
- Calculate remaining capacity for each slot
- Apply business rules (minimum 2-hour advance booking for today)
- Return list of available times

**Key Logic**:
- **Capacity Calculation**: If 3 barbers work simultaneously, each 30-minute slot can hold 3 bookings
- **Today's Special Case**: Only show slots starting 2+ hours from now
- **Tomorrow's Case**: Show all slots from opening time onwards
- **Closed Days**: Check if shop is closed (Sundays, etc.)

**Example Calculation**:
```
Shop has 3 barbers, opens 9 AM - 7 PM
Slot: 2:00 PM on Jan 9
Existing bookings at 2:00 PM: 2
Available capacity: 3 - 2 = 1 slot available ✅

Slot: 3:00 PM on Jan 9
Existing bookings at 3:00 PM: 3
Available capacity: 3 - 3 = 0 slots available ❌
```

---

### 4. BookingService
**Purpose**: Handle all booking operations

**Responsibilities**:
- Create new bookings
- Generate unique booking codes
- Validate booking requests (check capacity, not in past, within business hours)
- Retrieve customer's bookings by phone number
- Cancel bookings by booking code
- Calculate service end times
- Prevent double-booking through database constraints

**Key Logic**:
- **Validation Chain**: Check all rules before creating booking
- **Atomic Operations**: Database transaction ensures no race conditions
- **Booking Code Generation**: Random alphanumeric code (e.g., BK8472)
- **Status Tracking**: CONFIRMED, CANCELLED, COMPLETED, NO_SHOW

---

### 5. MessageFormatter
**Purpose**: Generate user-friendly WhatsApp messages

**Responsibilities**:
- Format service menus with emojis and pricing
- Display available time slots grouped by morning/afternoon
- Create confirmation messages with all booking details
- Generate error messages
- Format booking summaries

**Why Separate**: Keeps business logic clean and allows easy message template changes

---

## Class Diagram

```
┌─────────────────────────┐
│  WhatsAppController     │
│  (REST Controller)      │
│─────────────────────────│
│ + receiveMessage()      │
└───────────┬─────────────┘
            │ uses
            ▼
┌─────────────────────────┐
│  ConversationManager    │
│─────────────────────────│
│ - currentStep           │
│ - conversationData      │
│─────────────────────────│
│ + processMessage()      │
│ + loadState()           │
│ + saveState()           │
│ + transition()          │
└──┬──────────────────┬───┘
   │ uses             │ uses
   ▼                  ▼
┌──────────────┐  ┌────────────────┐
│Availability  │  │ BookingService │
│Service       │  │                │
│──────────────│  │────────────────│
│+ getSlots    │  │+ createBooking │
│  ForToday()  │  │+ validateSlot()│
│+ getSlots    │  │+ getBookings() │
│  ForTomorrow │  │+ cancelBooking │
│+ checkShop   │  │+ generateCode()│
│  Open()      │  │                │
└──┬───────────┘  └───┬────────────┘
   │ uses             │ uses
   ▼                  ▼
┌────────────────────────────┐
│  Repositories              │
│────────────────────────────│
│ BookingRepository          │
│ ServiceRepository          │
│ ConversationStateRepository│
└────────────┬───────────────┘
             │ reads/writes
             ▼
┌─────────────────────────┐
│      MySQL Database     │
│─────────────────────────│
│ - bookings              │
│ - services              │
│ - conversation_states   │
└─────────────────────────┘

        ┌──────────────────┐
        │ MessageFormatter │
        │──────────────────│
        │+ formatMenu()    │
        │+ formatSlots()   │
        │+ formatConfirm() │
        └──────────────────┘
               ▲
               │ used by all services
```

---

## Database Design

### Tables Overview

**services**
- Stores available haircut services
- Fields: id, name, duration_minutes, price, description, active

**bookings**
- Core booking records
- Fields: id, booking_code, customer_phone, service_id, booking_date, start_time, end_time, status, created_at
- Indexes on (booking_date, status) for fast availability queries

**conversation_states**
- Tracks where each customer is in the conversation flow
- Fields: id, phone_number, current_step, conversation_data (JSON), last_updated
- JSON stores temporary data: selected_service_id, selected_date, selected_time

### Key Relationships

```
services (1) ────< (many) bookings
                         └── each booking references one service

conversation_states ─── customer_phone matches bookings.customer_phone
                         (no foreign key, just lookup)
```

---

## Business Logic & Rules

### Availability Calculation Algorithm

**Problem**: How do we know if a time slot is available?

**Solution**:
1. Generate all possible 30-minute slots between opening and closing time
2. For each slot, count how many bookings already exist that overlap
3. Compare to shop capacity (number of barbers)
4. If bookings < capacity, slot is available

**Edge Cases**:
- Service duration spans multiple slots (45-min fade needs to check multiple 30-min windows)
- Current time is 1 PM, can't show 2 PM slot (only 1 hour notice)
- Tomorrow's date falls on Sunday (shop closed)

---

### Booking Validation Logic

**Problem**: How do we prevent invalid bookings?

**Validation Checklist**:
1. ✓ Selected time slot is available (not at capacity)
2. ✓ Booking date is today or tomorrow only
3. ✓ Booking time is not in the past
4. ✓ For today: booking time is at least 2 hours from now
5. ✓ Shop is open on that day/time
6. ✓ Service exists and is active
7. ✓ No duplicate booking (same customer, same time)

**If Validation Fails**: Return error message, keep customer in same conversation step

---

### Conversation State Management

**Problem**: WhatsApp is stateless. How do we remember where the customer is?

**Solution**: Database-backed state machine

**Flow**:
1. Customer sends message
2. Look up their phone number in conversation_states table
3. Load current_step (enum) and conversation_data (JSON)
4. Process message based on current step
5. Update state and save back to database
6. Return response

**Example State Data**:
```json
{
  "selected_service_id": 2,
  "selected_date": "2025-01-09",
  "selected_time": "14:30",
  "booking_code": "BK8472"
}
```

---

## Technical Decisions & Trade-offs

### Why Only Today/Tomorrow?

**Decision**: Restrict to today or tomorrow only

**Reasoning**:
- Barbershops have high walk-in traffic; long-term bookings less useful
- Simpler UX - no date picker needed
- Easier to calculate availability (only 2 days max)
- Encourages same-day bookings when possible
- MVP can validate demand before building complex scheduling

**Trade-off**: Can't book appointments for next week, but that's intentional for MVP

---

### Why 30-Minute Slot Intervals?

**Decision**: All slots are 30-minute intervals (10:00, 10:30, 11:00, etc.)

**Reasoning**:
- Services vary in length (20 min trim, 45 min fade)
- 30 minutes is common denominator
- Barber can finish early and start next customer
- Prevents awkward 10:17 AM time slots

**Trade-off**: Some wasted time if service takes 20 minutes, but simplicity wins

---

### Why JSON for Conversation Data?

**Decision**: Store conversation context as JSON blob instead of separate columns

**Reasoning**:
- Flexible schema - easy to add new data points
- Don't need separate tables for temporary data
- Natural fit for state machine pattern
- Easy to serialize/deserialize in Java (Jackson library)

**Trade-off**: Can't query inside JSON easily, but we don't need to for MVP

---

### Why Booking Codes Instead of IDs?

**Decision**: Generate alphanumeric codes (BK8472) instead of exposing database IDs

**Reasoning**:
- Easier for customers to read over WhatsApp
- Can't guess other booking IDs
- Professional appearance
- Short and memorable

**Trade-off**: Need unique constraint and generation logic, but worth it for UX

---

## Key Challenges & Solutions

### Challenge 1: Race Conditions (Double Booking)

**Problem**: Two customers book the last available slot at the exact same time

**Solution**: 
- Use database transactions with row-level locking
- Check availability inside transaction before inserting booking
- Database unique constraints on (booking_date, start_time, customer_phone)
- If constraint fails, return "slot just taken, please try another"

---

### Challenge 2: Time Zone Handling

**Problem**: Server might be in different timezone than Dublin barbershop

**Solution**:
- Store all times as LocalTime (no timezone) in database
- Assume everything is in shop's local timezone (Irish time)
- No need for UTC conversion in MVP
- Display times as-is (2:30 PM means 2:30 PM Irish time)

---

### Challenge 3: "Expired" Bookings

**Problem**: Database fills with old bookings over time

**Solution**:
- Scheduled job (Spring @Scheduled) runs daily at midnight
- Updates bookings from yesterday: status changes from CONFIRMED → COMPLETED
- Keeps data for analytics, but marks as done
- Option to purge bookings older than 90 days

---

### Challenge 4: Invalid User Input

**Problem**: Customer types "tree" instead of number, or invalid booking code

**Solution**:
- Validate input at every step
- If invalid, send friendly error message
- Keep customer in same conversation step
- Provide examples of valid input
- After 3 invalid attempts, reset to main menu

---

## Configuration Requirements

### Shop Settings (Hardcoded for MVP)

**Operating Hours**:
- Opening time: 9:00 AM
- Closing time: 7:00 PM
- Closed days: Sunday

**Capacity**:
- Number of barbers: 3
- Slot interval: 30 minutes
- Services run concurrently (all barbers work simultaneously)

**Business Rules**:
- Minimum advance booking for today: 2 hours
- Maximum advance booking: Tomorrow only
- No cancellation fee (can cancel anytime)

**Future Enhancement**: Move these to database table for easy configuration changes

---

## What You Need from the Barbershop

### Business Information

1. **Services & Pricing**
   - List all services offered
   - Duration for each service
   - Current pricing
   - Any services to exclude from online booking?

2. **Operating Schedule**
   - Opening and closing times
   - Days closed
   - Lunch breaks?
   - Different hours on weekends?

3. **Capacity**
   - How many barbers typically working?
   - Same number all days or varies?
   - Any solo shifts?

4. **Booking Rules**
   - How far in advance can people book currently?
   - Walk-in vs appointment ratio?
   - Cancellation policy?
   - Minimum notice needed?

5. **Contact**
   - WhatsApp Business number (or will you set one up?)
   - Barbershop address for confirmations
   - Owner/manager contact for admin notifications

---

## Development Phases

### Phase 1: Core Booking (Week 1)

**Goal**: Get basic booking flow working

**Tasks**:
1. Set up database schema
2. Implement AvailabilityService (slot calculation logic)
3. Build ConversationManager (state machine)
4. Create service selection and time selection flows
5. Implement booking creation and confirmation

**Deliverable**: Can book appointments through WhatsApp in sandbox environment

---

### Phase 2: View & Cancel (Week 1-2)

**Goal**: Add booking management features

**Tasks**:
1. Implement "View My Bookings" feature
2. Add cancellation flow with booking code validation
3. Build admin notification system (WhatsApp alerts to shop)
4. Add error handling and validation

**Deliverable**: Customers can manage their bookings

---

### Phase 3: Polish & Deploy (Week 2)

**Goal**: Make it production-ready

**Tasks**:
1. Add comprehensive error handling
2. Implement scheduled job for booking cleanup
3. Create simple admin dashboard (view today's schedule)
4. Deploy to cloud (Railway/Heroku)
5. Set up Twilio production WhatsApp API
6. Test with real scenarios

**Deliverable**: Live system ready for pilot

---

## Success Metrics

### Technical Metrics
- **Response Time**: < 2 seconds per message
- **Uptime**: > 99% availability
- **Error Rate**: < 1% of conversations encounter errors

### Business Metrics
- **Booking Conversion**: % of customers who complete booking after starting
- **Slot Utilization**: % of available slots that get booked
- **Cancellation Rate**: % of bookings that get cancelled
- **Peak Usage Times**: When do most bookings happen?

### Customer Satisfaction
- **Response Clarity**: Do customers understand the flow?
- **Booking Success**: Can customers book on first try?
- **Issue Rate**: How many customers report problems?

---

## Pilot Strategy

### Week 1-2: Internal Testing
- Use Twilio sandbox with your personal number
- Test all flows thoroughly
- Fix bugs and improve messages

### Week 3-4: Soft Launch
- Deploy to production Twilio
- Shop staff test with their phones
- Book fake appointments, test cancellations
- Get feedback on flow and messaging

### Month 2: Limited Pilot
- Advertise to small group (e.g., sign in shop window)
- Monitor bookings closely
- Gather customer feedback
- Track metrics daily

### Month 3: Full Launch
- Promote on shop's social media
- Update Google Business profile
- Consider paid ads targeting "barber near me"
- Scale based on demand

---

## Risks & Mitigations

### Risk 1: Customers Don't Trust WhatsApp Booking

**Mitigation**: 
- Keep phone line open for traditional bookings
- Add "As seen on WhatsApp" poster in shop
- Staff mentions it to every customer

### Risk 2: Too Many No-Shows

**Mitigation**:
- Track no-show rate
- If >20%, add Phase 2 feature: automated reminders 1 hour before
- Consider requiring name in addition to phone number

### Risk 3: System Goes Down During Peak Hours

**Mitigation**:
- Host on reliable platform (Railway/Heroku with auto-scaling)
- Set up uptime monitoring (UptimeRobot, free)
- Have fallback: staff can manually check database and book customers

### Risk 4: Barbers Forget to Check Bookings

**Mitigation**:
- Send WhatsApp notification to shop number for every new booking
- Build simple admin dashboard showing today's schedule
- Daily morning summary sent automatically

---

## Cost Analysis

### Development Costs (Your Time)
- Week 1: Core booking system (20-30 hours)
- Week 2: Polish and deploy (10-15 hours)
- **Total**: 30-45 hours of development

### Operational Costs (Monthly)
- **Hosting**: Railway/Heroku - €10-15/month
- **Database**: MySQL on Railway - €5-10/month  
- **Twilio WhatsApp API**: ~€40-60/month (depends on message volume)
- **Domain** (optional): €1/month
- **Total**: €60-85/month

### Pricing for Barbershop
- **Setup Fee**: €300-400 (covers your development time)
- **Monthly Fee**: €59/month

**Or Alternative Model**:
- No setup fee
- €0.75 per completed booking

**Example**: 180 bookings/month = €135 in booking fees

**Break-even**: You recoup development costs after 1-2 pilot shops

---

## Tech Stack

### Backend
- **Java Spring Boot** - Core application framework
- **Spring Data JPA** - Database access
- **MySQL** - Relational database
- **Jackson** - JSON serialization

### External Services
- **Twilio WhatsApp Business API** - Messaging
- **Railway/Heroku** - Cloud hosting

### Development Tools
- **ngrok** - Local webhook testing
- **Postman** - API testing
- **Git** - Version control

---

## Next Steps

1. **Meet with barber** - Show this README, get their input
2. **Gather requirements** - Fill in the "What You Need" section above
3. **Set up development environment** - Configure database and dependencies
4. **Build Phase 1** - Get core booking flow working
5. **Demo in person** - Show working prototype on your phone

---

## Questions for Your Barber

- *"Would 'today or tomorrow only' work for your business, or do customers need to book further ahead?"*
- *"What's your busiest day? Should we limit slots on Saturdays?"*
- *"Do you prefer customers booking exact times (2:30 PM) or broader windows (afternoon)?"*
- *"What happens if someone books and doesn't show - is that a big problem?"*
- *"Do you have a WhatsApp Business account already, or would you need to set one up?"*

---

## Contact

**Developer**: Banda
**Project Timeline**: 2 weeks for MVP
**Next Milestone**: Working prototype demo

---

*This simplified MVP can be built in 2 weeks and immediately validates if WhatsApp booking solves a real problem for Irish barbershops!* 🚀
