/*  
 * This program tests the operation of a register
 * composed of a 74HC574AP tri-state flip-flop and
 * SN74LS35 tri-state transciever, as shown in the
 * scheme file '[ProjectDir]/Schematics/register.png'.
 * 
 * Random numbers are generated and stored in the
 * register, then retrieved and compared to the original number.
 *
 * Created by Lorenzo Notaro, 2019.
 */

#include <Computer8Bit.h>

/* The pins dedicated to the three control signals required by the register,
r_disable, r_in. */
#define R_DISABLE        11
#define R_IN             12

/* Utility macro to run code only if verbose mode is on. */
#define REQUIRE_VERBOSE(code) if(verbose_mode) code

#define EXPAND_BUS(var) var[0], var[1], var[2], var[3], var[4], var[5], var[6], var[7]

#define SEPARATOR "=================="

Computer8Bit computer(10, 2, 3, 4, 5, 6, 7, 8, 9);

void setup() {

  Serial.begin(9600);
  
  pinMode(R_DISABLE,OUTPUT);
  pinMode(R_IN, OUTPUT);

  digitalWrite(R_DISABLE, HIGH);

  computer.writeToBus(NULL);
}

bool do_test(int test_count, signed char verbose_mode){
  byte gen_number, num;
  bool temp[8];
    gen_number = random(255);
    tobinary(gen_number, temp);
    
    REQUIRE_VERBOSE({
      serialPrintf("Writing random value %d (%d%d%d%d%d%d%d%d) to register...\n", gen_number, EXPAND_BUS(temp));
    });
    digitalWrite(R_IN, HIGH);
    digitalWrite(R_DISABLE, LOW);
    computer.writeToBus(temp);
    computer.clockPulse(100);
    digitalWrite(R_DISABLE, HIGH);
    computer.writeToBus(temp);
    computer.busMode(INPUT);
    REQUIRE_VERBOSE({
        Serial.print("Retrieving register contents...\n");
    });

    digitalWrite(R_IN, LOW);
    digitalWrite(R_DISABLE, LOW);
    computer.clock(HIGH);
    computer.readFromBus(temp);
    computer.clock(LOW);
    num = tobyte(temp);
    
    if(num == gen_number){
      REQUIRE_VERBOSE({        
        serialPrintf("Register responded with the CORRECT value, %d (%d%d%d%d%d%d%d%d).\n\n", num,  EXPAND_BUS(temp));
      });
      return true;
    }else{
      REQUIRE_VERBOSE({        
        serialPrintf("Register responded with the WRONG value, %d (%d%d%d%d%d%d%d%d).\n\n", num, EXPAND_BUS(temp));
      });
      return false;
    }
}


void get_input(int *test_count, signed char *verbose_mode){
  Serial.println(SEPARATOR "\nRegister tester.\n" SEPARATOR);
  while(*test_count == -1){
     serialFlush();
     Serial.print("How many tests would you like to run (>0)? ");
     while(!Serial.available());
     *test_count = Serial.parseInt();
     Serial.println(*test_count);
     if(*test_count <= 0){
      *test_count = -1;
      Serial.print("Invalid value. ");
     }
  }
  while(*verbose_mode == -1){
    serialFlush();
    Serial.print("Verbose output (y/n)? ");
    while(!Serial.available());
    char c = Serial.read();
    Serial.println(c);
    if(c == 'y' || c == 'Y'){
      *verbose_mode = 1;
    }else if(c == 'n' || c == 'N'){
      *verbose_mode = 0;
    }else{
      Serial.print("Invalid input. ");
    }
  }
}

void loop(){
  int i;
  int test_count = -1;
  signed char verbose_mode = -1;
  unsigned int correct = 0;
  unsigned int wrong = 0;
  get_input(&test_count, &verbose_mode);
  serialPrintf("Running %d test(s)...\n", test_count);
  long startTime = micros();
  for(i = 1; i <= test_count; ++i){
    REQUIRE_VERBOSE({
      serialPrintf("=====TEST %d=====\n", i);
    });
    ++(*(do_test(test_count, verbose_mode) ? &correct : &wrong));
  }
   
  long duration = (micros() - startTime);
  Serial.println(SEPARATOR);
  serialPrintf("Test results: \n    %d/%d correct\n    %d/%d incorrect\n", correct, test_count, wrong, test_count);
  Serial.print("    (");
  Serial.print((float) correct * 100.0 / (correct + wrong));
  Serial.println("% correct)");
  Serial.print("    Time elapsed: ");
  Serial.print((double) duration / 1000.0);
  Serial.println("ms");
  Serial.println(SEPARATOR);

  computer.writeToBus(NULL);
}
