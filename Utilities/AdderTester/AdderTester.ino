#define PORT_BITMASK B00011110

#define PULSE_DURATION            1

#define SUBTRACT                  2
#define CLOCK                     3
#define ADDER_NOT_OE              4
#define B_NOT_OE                  5
#define A_NOT_OE                  6

#define TEST_COUNT             10000
#define VERBOSE false

int8_t readBus();
void writeBus(int8_t value);
void pulse(int pin);
void lowPulse(int pin);
void busMode(int mode);
bool do_test(int8_t a, int8_t b, bool sub);
void setup() {
  randomSeed(analogRead(A7));
  Serial.begin(9600);
  pinMode(SUBTRACT, OUTPUT);
  pinMode(CLOCK, OUTPUT);
  pinMode(ADDER_NOT_OE, OUTPUT);
  pinMode(A_NOT_OE, OUTPUT);
  pinMode(B_NOT_OE, OUTPUT);

  digitalWrite(CLOCK, LOW);
  digitalWrite(SUBTRACT, LOW);
  digitalWrite(ADDER_NOT_OE, HIGH);
  digitalWrite(A_NOT_OE, HIGH);
  digitalWrite(B_NOT_OE, HIGH);

  int i;
  int8_t a, b;
  int success = 0;
  for(i = 0; i < TEST_COUNT; ++i){
      a = (int8_t) random(-128, 128);
      b = (int8_t) random(-128, 128);
      if(do_test(a, b, random(0, 2)))
        ++success;
  }
  //do_test(5, 10, false);
  Serial.println("===========================");
  Serial.print(TEST_COUNT);
  Serial.print(" tests done; ");
  Serial.print(success);
  Serial.print("(");
  Serial.print((float) success / (float) TEST_COUNT * 100);
  Serial.println("%) were successful.");
  Serial.println("===========================");
}

void serial_print_valf(int8_t val){
    Serial.print(val);
    Serial.print(" (");
    for(int i = 0; i < 8; ++i){
      Serial.print((val >> (7 - i)) & 0x1 == 1 ? "1" : "0"); 
    }
    Serial.print(")");
}

boolean do_test(int8_t a, int8_t b, bool sub){
  if(VERBOSE){
    serial_print_valf(a);
    Serial.print(sub ? " - " : " + ");
    serial_print_valf(b);
    Serial.print(" = ");
  }
  
  busMode(OUTPUT);
  //write to A
  writeBus(a);
  digitalWrite(A_NOT_OE, LOW);
  pulse(CLOCK);
  digitalWrite(A_NOT_OE, HIGH);

  //write to B
  writeBus(b);
  digitalWrite(B_NOT_OE, LOW);
  pulse(CLOCK);
  digitalWrite(B_NOT_OE, HIGH);

  busMode(INPUT);
  //sum/sub
  //digitalWrite(CLOCK, HIGH);
  digitalWrite(A_NOT_OE, LOW);
  digitalWrite(B_NOT_OE, LOW);
  if(sub)
    digitalWrite(SUBTRACT, HIGH);
  digitalWrite(ADDER_NOT_OE, LOW);
  int8_t val = readBus();
//  delay(1000);
  digitalWrite(ADDER_NOT_OE, HIGH);
  if(sub)
    digitalWrite(SUBTRACT, LOW);
  digitalWrite(B_NOT_OE, HIGH);
  digitalWrite(A_NOT_OE, HIGH);
  //digitalWrite(CLOCK, LOW);

  int8_t answer = (sub ? (a - b) : (a + b));
  bool success = val == answer;
  if(VERBOSE){
    serial_print_valf(val);
    if(!success){
      Serial.print(" --- WRONG! Should be ");
      serial_print_valf(answer);
    }
    Serial.println();
  }
  return success;
}


void loop() {
  // put your main code here, to run repeatedly:

}

int8_t readBus(){
    return (int8_t) ((PINB & PORT_BITMASK) >> 1) + ((PINC & PORT_BITMASK) << 3);
}

void writeBus(int8_t val){
  PORTB = (val << 1) & PORT_BITMASK;
  PORTC = (val >> 3) & PORT_BITMASK;
}

void busMode(int mode){
  DDRC = DDRB = mode == OUTPUT ? PORT_BITMASK : ~PORT_BITMASK;  
}

void pulse(int pin){
  digitalWrite(pin, HIGH);
  delay(PULSE_DURATION);
  digitalWrite(pin, LOW);
}

void lowPulse(int pin){
  digitalWrite(pin, LOW);
  delayMicroseconds(PULSE_DURATION);
  digitalWrite(pin, HIGH);
}
